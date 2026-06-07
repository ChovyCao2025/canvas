package org.chovy.canvas.domain.loyalty;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.chovy.canvas.dal.dataobject.LoyaltyMemberAccountDO;
import org.chovy.canvas.dal.dataobject.LoyaltyRedemptionDO;
import org.chovy.canvas.dal.dataobject.LoyaltyRuleDO;
import org.chovy.canvas.dal.dataobject.LoyaltyTransactionJournalDO;
import org.chovy.canvas.dal.mapper.LoyaltyMemberAccountMapper;
import org.chovy.canvas.dal.mapper.LoyaltyRedemptionMapper;
import org.chovy.canvas.dal.mapper.LoyaltyRuleMapper;
import org.chovy.canvas.dal.mapper.LoyaltyTransactionJournalMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class LoyaltyService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REDEEMED = "REDEEMED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String TIER_BASIC = "BASIC";
    private static final String TIER_SILVER = "SILVER";
    private static final String TIER_GOLD = "GOLD";
    private static final String TIER_PLATINUM = "PLATINUM";

    private final LoyaltyMemberAccountMapper accountMapper;
    private final LoyaltyTransactionJournalMapper journalMapper;
    private final LoyaltyRedemptionMapper redemptionMapper;
    private final LoyaltyRuleMapper ruleMapper;
    private final Clock clock;

    @Autowired
    public LoyaltyService(LoyaltyMemberAccountMapper accountMapper,
                          LoyaltyTransactionJournalMapper journalMapper,
                          LoyaltyRedemptionMapper redemptionMapper,
                          LoyaltyRuleMapper ruleMapper) {
        this(accountMapper, journalMapper, redemptionMapper, ruleMapper, Clock.systemDefaultZone());
    }

    LoyaltyService(LoyaltyMemberAccountMapper accountMapper,
                   LoyaltyTransactionJournalMapper journalMapper,
                   LoyaltyRedemptionMapper redemptionMapper,
                   LoyaltyRuleMapper ruleMapper,
                   Clock clock) {
        this.accountMapper = accountMapper;
        this.journalMapper = journalMapper;
        this.redemptionMapper = redemptionMapper;
        this.ruleMapper = ruleMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public LoyaltyAccountView account(Long tenantId, String userId) {
        return toAccountView(requireAccount(tenantId, userId));
    }

    @Transactional
    public LoyaltyAccountView earn(Long tenantId, String userId, EarnCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedUserId = requireText(userId, "loyalty user id is required");
        EarnCommand req = command == null ? new EarnCommand(null, null, null, null, null, null, null) : command;
        String transactionKey = requireText(req.transactionKey(), "loyalty earn transaction key is required");
        int points = positive(req.points(), "loyalty earn points must be positive");
        LoyaltyMemberAccountDO account = findAccount(scopedTenantId, scopedUserId);
        if (account == null) {
            account = newAccount(scopedTenantId, scopedUserId);
        }
        LoyaltyTransactionJournalDO existing = journalByKey(scopedTenantId, transactionKey);
        if (existing != null) {
            return toAccountView(account);
        }
        applyBalance(account, points);
        if (account.getId() == null) {
            accountMapper.insert(account);
        } else {
            accountMapper.updateById(account);
        }
        journalMapper.insert(journal(account, transactionKey, "EARN", points,
                defaultString(req.pointsType(), "BASE"), req.sourceType(), req.sourceId(), req.reason(), req.expiresAt()));
        return toAccountView(account);
    }

    @Transactional
    public RedemptionView redeem(Long tenantId, String userId, RedemptionCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedUserId = requireText(userId, "loyalty user id is required");
        RedemptionCommand req = command == null
                ? new RedemptionCommand(null, null, null, null)
                : command;
        String redemptionKey = requireText(req.redemptionKey(), "loyalty redemption key is required");
        String rewardKey = requireText(req.rewardKey(), "loyalty reward key is required");
        int pointsCost = positive(req.pointsCost(), "loyalty redemption points cost must be positive");
        LoyaltyRedemptionDO existing = redemptionByKey(scopedTenantId, redemptionKey);
        if (existing != null) {
            return toRedemptionView(existing);
        }
        LoyaltyMemberAccountDO account = requireAccount(scopedTenantId, scopedUserId);
        if (value(account.getPointsBalance()) < pointsCost) {
            LoyaltyRedemptionDO failed = redemption(account, redemptionKey, rewardKey, pointsCost,
                    STATUS_FAILED, "insufficient loyalty points balance", req.reason());
            redemptionMapper.insert(failed);
            return toRedemptionView(failed);
        }
        applyBalance(account, -pointsCost);
        accountMapper.updateById(account);
        LoyaltyRedemptionDO redeemed = redemption(account, redemptionKey, rewardKey, pointsCost,
                STATUS_REDEEMED, null, req.reason());
        redemptionMapper.insert(redeemed);
        journalMapper.insert(journal(account, "redemption:" + redemptionKey, "REDEEM", -pointsCost,
                "BASE", "REWARD", rewardKey, req.reason(), null));
        return toRedemptionView(redeemed);
    }

    public List<BenefitEligibilityView> eligibleBenefits(Long tenantId, String userId) {
        LoyaltyMemberAccountDO account = requireAccount(tenantId, userId);
        int tierRank = tierRank(account.getTierCode());
        return rules(normalizeTenant(tenantId), "BENEFIT").stream()
                .filter(rule -> value(rule.getEnabled()) != 0)
                .filter(rule -> tierRank >= tierRank(defaultString(rule.getMinTierCode(), TIER_BASIC)))
                .sorted(Comparator.comparing(rule -> defaultString(rule.getBenefitKey(), rule.getRuleKey())))
                .map(rule -> new BenefitEligibilityView(
                        defaultString(rule.getBenefitKey(), rule.getRuleKey()),
                        defaultString(rule.getBenefitName(), rule.getRuleKey()),
                        defaultString(rule.getMinTierCode(), TIER_BASIC),
                        true,
                        "tier eligible"))
                .toList();
    }

    private LoyaltyMemberAccountDO requireAccount(Long tenantId, String userId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedUserId = requireText(userId, "loyalty user id is required");
        LoyaltyMemberAccountDO account = findAccount(scopedTenantId, scopedUserId);
        if (account == null) {
            account = newAccount(scopedTenantId, scopedUserId);
            accountMapper.insert(account);
        }
        return account;
    }

    private LoyaltyMemberAccountDO findAccount(Long tenantId, String userId) {
        return accountMapper.selectOne(new QueryWrapper<LoyaltyMemberAccountDO>()
                .eq("tenant_id", tenantId)
                .eq("user_id", userId)
                .last("LIMIT 1"));
    }

    private LoyaltyTransactionJournalDO journalByKey(Long tenantId, String transactionKey) {
        return journalMapper.selectOne(new QueryWrapper<LoyaltyTransactionJournalDO>()
                .eq("tenant_id", tenantId)
                .eq("transaction_key", transactionKey)
                .last("LIMIT 1"));
    }

    private LoyaltyRedemptionDO redemptionByKey(Long tenantId, String redemptionKey) {
        return redemptionMapper.selectOne(new QueryWrapper<LoyaltyRedemptionDO>()
                .eq("tenant_id", tenantId)
                .eq("redemption_key", redemptionKey)
                .last("LIMIT 1"));
    }

    private List<LoyaltyRuleDO> rules(Long tenantId, String ruleType) {
        List<LoyaltyRuleDO> rows = ruleMapper.selectList(new QueryWrapper<LoyaltyRuleDO>()
                .eq("tenant_id", tenantId)
                .eq("rule_type", ruleType)
                .eq("enabled", 1)
                .orderByAsc("id"));
        return rows == null ? List.of() : rows;
    }

    private LoyaltyMemberAccountDO newAccount(Long tenantId, String userId) {
        LocalDateTime now = now();
        LoyaltyMemberAccountDO row = new LoyaltyMemberAccountDO();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setMemberNo("M-" + tenantId + "-" + userId);
        row.setTierCode(TIER_BASIC);
        row.setPointsBalance(0);
        row.setLifetimePoints(0);
        row.setStatus(STATUS_ACTIVE);
        row.setEnrolledAt(now);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        return row;
    }

    private void applyBalance(LoyaltyMemberAccountDO account, int delta) {
        account.setPointsBalance(Math.max(0, value(account.getPointsBalance()) + delta));
        if (delta > 0) {
            account.setLifetimePoints(value(account.getLifetimePoints()) + delta);
        }
        account.setTierCode(tierForLifetime(value(account.getLifetimePoints())));
        account.setStatus(defaultString(account.getStatus(), STATUS_ACTIVE));
        account.setUpdatedAt(now());
    }

    private LoyaltyTransactionJournalDO journal(LoyaltyMemberAccountDO account,
                                               String transactionKey,
                                               String transactionType,
                                               int pointsDelta,
                                               String pointsType,
                                               String sourceType,
                                               String sourceId,
                                               String reason,
                                               LocalDateTime expiresAt) {
        LoyaltyTransactionJournalDO row = new LoyaltyTransactionJournalDO();
        row.setTenantId(account.getTenantId());
        row.setUserId(account.getUserId());
        row.setAccountId(account.getId());
        row.setTransactionKey(transactionKey);
        row.setTransactionType(transactionType);
        row.setPointsDelta(pointsDelta);
        row.setPointsType(pointsType);
        row.setBalanceAfter(value(account.getPointsBalance()));
        row.setSourceType(sourceType);
        row.setSourceId(sourceId);
        row.setReason(reason);
        row.setOccurredAt(now());
        row.setExpiresAt(expiresAt);
        row.setCreatedAt(now());
        return row;
    }

    private LoyaltyRedemptionDO redemption(LoyaltyMemberAccountDO account,
                                           String redemptionKey,
                                           String rewardKey,
                                           int pointsCost,
                                           String status,
                                           String failureReason,
                                           String reason) {
        LocalDateTime now = now();
        LoyaltyRedemptionDO row = new LoyaltyRedemptionDO();
        row.setTenantId(account.getTenantId());
        row.setUserId(account.getUserId());
        row.setAccountId(account.getId());
        row.setRedemptionKey(redemptionKey);
        row.setRewardKey(rewardKey);
        row.setPointsCost(pointsCost);
        row.setStatus(status);
        row.setFailureReason(failureReason == null ? reason : failureReason);
        row.setRedeemedAt(now);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        return row;
    }

    private LoyaltyAccountView toAccountView(LoyaltyMemberAccountDO row) {
        return new LoyaltyAccountView(
                row.getId(),
                row.getTenantId(),
                row.getUserId(),
                row.getMemberNo(),
                defaultString(row.getTierCode(), TIER_BASIC),
                value(row.getPointsBalance()),
                value(row.getLifetimePoints()),
                defaultString(row.getStatus(), STATUS_ACTIVE));
    }

    private RedemptionView toRedemptionView(LoyaltyRedemptionDO row) {
        return new RedemptionView(
                row.getId(),
                row.getRedemptionKey(),
                row.getRewardKey(),
                value(row.getPointsCost()),
                row.getStatus(),
                row.getFailureReason(),
                row.getRedeemedAt());
    }

    private String tierForLifetime(int lifetimePoints) {
        if (lifetimePoints >= 10_000) {
            return TIER_PLATINUM;
        }
        if (lifetimePoints >= 1_000) {
            return TIER_GOLD;
        }
        if (lifetimePoints >= 100) {
            return TIER_SILVER;
        }
        return TIER_BASIC;
    }

    private int tierRank(String tierCode) {
        return switch (defaultString(tierCode, TIER_BASIC).toUpperCase(Locale.ROOT)) {
            case TIER_PLATINUM -> 3;
            case TIER_GOLD -> 2;
            case TIER_SILVER -> 1;
            default -> 0;
        };
    }

    private int positive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public record EarnCommand(
            String transactionKey,
            Integer points,
            String pointsType,
            String sourceType,
            String sourceId,
            String reason,
            LocalDateTime expiresAt) {
    }

    public record RedemptionCommand(
            String redemptionKey,
            String rewardKey,
            Integer pointsCost,
            String reason) {
    }

    public record LoyaltyAccountView(
            Long accountId,
            Long tenantId,
            String userId,
            String memberNo,
            String tierCode,
            int pointsBalance,
            int lifetimePoints,
            String status) {
    }

    public record RedemptionView(
            Long redemptionId,
            String redemptionKey,
            String rewardKey,
            int pointsCost,
            String status,
            String failureReason,
            LocalDateTime redeemedAt) {
    }

    public record BenefitEligibilityView(
            String benefitKey,
            String benefitName,
            String minTierCode,
            boolean eligible,
            String reason) {
    }
}
