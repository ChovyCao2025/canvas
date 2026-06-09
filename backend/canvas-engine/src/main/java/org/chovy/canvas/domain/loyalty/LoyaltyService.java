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
/**
 * LoyaltyService 承载对应领域的业务规则、流程编排和结果转换。
 */
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
    /**
     * 初始化 LoyaltyService 实例。
     *
     * @param accountMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param journalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param redemptionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param ruleMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public LoyaltyService(LoyaltyMemberAccountMapper accountMapper,
                          LoyaltyTransactionJournalMapper journalMapper,
                          LoyaltyRedemptionMapper redemptionMapper,
                          LoyaltyRuleMapper ruleMapper) {
        this(accountMapper, journalMapper, redemptionMapper, ruleMapper, Clock.systemDefaultZone());
    }

    /**
     * 初始化 LoyaltyService 实例。
     *
     * @param accountMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param journalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param redemptionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param ruleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 account 流程生成的业务结果。
     */
    public LoyaltyAccountView account(Long tenantId, String userId) {
        return toAccountView(requireAccount(tenantId, userId));
    }

    @Transactional
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 earn 流程生成的业务结果。
     */
    public LoyaltyAccountView earn(Long tenantId, String userId, EarnCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedUserId = requireText(userId, "loyalty user id is required");
        EarnCommand req = command == null ? new EarnCommand(null, null, null, null, null, null, null) : command;
        String transactionKey = requireText(req.transactionKey(), "loyalty earn transaction key is required");
        int points = positive(req.points(), "loyalty earn points must be positive");
        LoyaltyMemberAccountDO account = findAccount(scopedTenantId, scopedUserId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (account == null) {
            account = newAccount(scopedTenantId, scopedUserId);
        }
        LoyaltyTransactionJournalDO existing = journalByKey(scopedTenantId, transactionKey);
        if (existing != null) {
            return toAccountView(account);
        }
        applyBalance(account, points);
        if (account.getId() == null) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            accountMapper.insert(account);
        } else {
            accountMapper.updateById(account);
        }
        journalMapper.insert(journal(account, transactionKey, "EARN", points,
                defaultString(req.pointsType(), "BASE"), req.sourceType(), req.sourceId(), req.reason(), req.expiresAt()));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toAccountView(account);
    }

    @Transactional
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 redeem 流程生成的业务结果。
     */
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
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (existing != null) {
            return toRedemptionView(existing);
        }
        LoyaltyMemberAccountDO account = requireAccount(scopedTenantId, scopedUserId);
        if (value(account.getPointsBalance()) < pointsCost) {
            LoyaltyRedemptionDO failed = redemption(account, redemptionKey, rewardKey, pointsCost,
                    STATUS_FAILED, "insufficient loyalty points balance", req.reason());
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toRedemptionView(redeemed);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 eligible benefits 汇总后的集合、分页或映射视图。
     */
    public List<BenefitEligibilityView> eligibleBenefits(Long tenantId, String userId) {
        // 准备本次处理所需的上下文和中间变量。
        LoyaltyMemberAccountDO account = requireAccount(tenantId, userId);
        int tierRank = tierRank(account.getTierCode());
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireAccount 流程生成的业务结果。
     */
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    private LoyaltyMemberAccountDO findAccount(Long tenantId, String userId) {
        return accountMapper.selectOne(new QueryWrapper<LoyaltyMemberAccountDO>()
                .eq("tenant_id", tenantId)
                .eq("user_id", userId)
                .last("LIMIT 1"));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param transactionKey 业务键，用于在同一租户下定位资源。
     * @return 返回 journalByKey 流程生成的业务结果。
     */
    private LoyaltyTransactionJournalDO journalByKey(Long tenantId, String transactionKey) {
        return journalMapper.selectOne(new QueryWrapper<LoyaltyTransactionJournalDO>()
                .eq("tenant_id", tenantId)
                .eq("transaction_key", transactionKey)
                .last("LIMIT 1"));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param redemptionKey 业务键，用于在同一租户下定位资源。
     * @return 返回 redemptionByKey 流程生成的业务结果。
     */
    private LoyaltyRedemptionDO redemptionByKey(Long tenantId, String redemptionKey) {
        return redemptionMapper.selectOne(new QueryWrapper<LoyaltyRedemptionDO>()
                .eq("tenant_id", tenantId)
                .eq("redemption_key", redemptionKey)
                .last("LIMIT 1"));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param ruleType 类型标识，用于选择对应处理分支。
     * @return 返回 rules 汇总后的集合、分页或映射视图。
     */
    private List<LoyaltyRuleDO> rules(Long tenantId, String ruleType) {
        List<LoyaltyRuleDO> rows = ruleMapper.selectList(new QueryWrapper<LoyaltyRuleDO>()
                .eq("tenant_id", tenantId)
                .eq("rule_type", ruleType)
                .eq("enabled", 1)
                .orderByAsc("id"));
        return rows == null ? List.of() : rows;
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 newAccount 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param account account 参数，用于 applyBalance 流程中的校验、计算或对象转换。
     * @param delta delta 参数，用于 applyBalance 流程中的校验、计算或对象转换。
     */
    private void applyBalance(LoyaltyMemberAccountDO account, int delta) {
        account.setPointsBalance(Math.max(0, value(account.getPointsBalance()) + delta));
        if (delta > 0) {
            account.setLifetimePoints(value(account.getLifetimePoints()) + delta);
        }
        account.setTierCode(tierForLifetime(value(account.getLifetimePoints())));
        account.setStatus(defaultString(account.getStatus(), STATUS_ACTIVE));
        account.setUpdatedAt(now());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param account account 参数，用于 journal 流程中的校验、计算或对象转换。
     * @param transactionKey 业务键，用于在同一租户下定位资源。
     * @param transactionType 类型标识，用于选择对应处理分支。
     * @param pointsDelta points delta 参数，用于 journal 流程中的校验、计算或对象转换。
     * @param pointsType 类型标识，用于选择对应处理分支。
     * @param sourceType 类型标识，用于选择对应处理分支。
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param expiresAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 journal 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param account account 参数，用于 redemption 流程中的校验、计算或对象转换。
     * @param redemptionKey 业务键，用于在同一租户下定位资源。
     * @param rewardKey 业务键，用于在同一租户下定位资源。
     * @param pointsCost points cost 参数，用于 redemption 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param failureReason 原因说明，用于记录状态变化的业务依据。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回 redemption 流程生成的业务结果。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param lifetimePoints 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 tier for lifetime 生成的文本或业务键。
     */
    private String tierForLifetime(int lifetimePoints) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (lifetimePoints >= 10_000) {
            return TIER_PLATINUM;
        }
        if (lifetimePoints >= 1_000) {
            return TIER_GOLD;
        }
        if (lifetimePoints >= 100) {
            return TIER_SILVER;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return TIER_BASIC;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tierCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 tier rank 计算得到的数量、金额或指标值。
     */
    private int tierRank(String tierCode) {
        return switch (defaultString(tierCode, TIER_BASIC).toUpperCase(Locale.ROOT)) {
            case TIER_PLATINUM -> 3;
            case TIER_GOLD -> 2;
            case TIER_SILVER -> 1;
            default -> 0;
        };
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回 positive 计算得到的数量、金额或指标值。
     */
    private int positive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回 require text 生成的文本或业务键。
     */
    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * EarnCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record EarnCommand(
            String transactionKey,
            Integer points,
            String pointsType,
            String sourceType,
            String sourceId,
            String reason,
            LocalDateTime expiresAt) {
    }

    /**
     * RedemptionCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RedemptionCommand(
            String redemptionKey,
            String rewardKey,
            Integer pointsCost,
            String reason) {
    }

    /**
     * LoyaltyAccountView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * RedemptionView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RedemptionView(
            Long redemptionId,
            String redemptionKey,
            String rewardKey,
            int pointsCost,
            String status,
            String failureReason,
            LocalDateTime redeemedAt) {
    }

    /**
     * BenefitEligibilityView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record BenefitEligibilityView(
            String benefitKey,
            String benefitName,
            String minTierCode,
            boolean eligible,
            String reason) {
    }
}
