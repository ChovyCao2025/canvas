package org.chovy.canvas.marketing.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.LoyaltyFacade.BenefitEligibilityView;
import org.chovy.canvas.marketing.api.LoyaltyFacade.EarnCommand;
import org.chovy.canvas.marketing.api.LoyaltyFacade.LoyaltyAccountView;
import org.chovy.canvas.marketing.api.LoyaltyFacade.RedemptionCommand;
import org.chovy.canvas.marketing.api.LoyaltyFacade.RedemptionView;

public class LoyaltyCatalog {

    private static final String BASIC = "BASIC";
    private static final String SILVER = "SILVER";
    private static final String GOLD = "GOLD";
    private static final String PLATINUM = "PLATINUM";

    private final Clock clock;
    private final Map<AccountKey, AccountRow> accounts = new LinkedHashMap<>();
    private final Map<TenantTransactionKey, String> earnedTransactionKeys = new LinkedHashMap<>();
    private final Map<TenantTransactionKey, RedemptionRow> redemptions = new LinkedHashMap<>();
    private long accountIds;
    private long redemptionIds;

    public LoyaltyCatalog(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public synchronized LoyaltyAccountView account(Long tenantId, String userId) {
        return view(requireAccount(tenantId, userId));
    }

    public synchronized LoyaltyAccountView earn(Long tenantId, String userId, EarnCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedUserId = requireText(userId, "loyalty user id is required");
        EarnCommand safe = command == null ? new EarnCommand(null, null, null, null, null, null, null) : command;
        String transactionKey = requireText(safe.transactionKey(), "loyalty earn transaction key is required");
        int points = positive(safe.points(), "loyalty earn points must be positive");
        AccountRow account = requireAccount(scopedTenantId, scopedUserId);
        TenantTransactionKey key = new TenantTransactionKey(scopedTenantId, transactionKey);
        if (earnedTransactionKeys.containsKey(key)) {
            return view(account);
        }
        earnedTransactionKeys.put(key, scopedUserId);
        account.pointsBalance += points;
        account.lifetimePoints += points;
        account.tierCode = tierForLifetime(account.lifetimePoints);
        account.updatedAt = now();
        return view(account);
    }

    public synchronized RedemptionView redeem(Long tenantId, String userId, RedemptionCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedUserId = requireText(userId, "loyalty user id is required");
        RedemptionCommand safe = command == null ? new RedemptionCommand(null, null, null, null) : command;
        String redemptionKey = requireText(safe.redemptionKey(), "loyalty redemption key is required");
        String rewardKey = requireText(safe.rewardKey(), "loyalty reward key is required");
        int pointsCost = positive(safe.pointsCost(), "loyalty redemption points cost must be positive");
        TenantTransactionKey key = new TenantTransactionKey(scopedTenantId, redemptionKey);
        RedemptionRow existing = redemptions.get(key);
        if (existing != null) {
            return view(existing);
        }
        AccountRow account = requireAccount(scopedTenantId, scopedUserId);
        String status = "REDEEMED";
        String failureReason = null;
        if (account.pointsBalance < pointsCost) {
            status = "FAILED";
            failureReason = "insufficient loyalty points balance";
        } else {
            account.pointsBalance -= pointsCost;
            account.updatedAt = now();
        }
        RedemptionRow row = new RedemptionRow(++redemptionIds, redemptionKey, rewardKey, pointsCost, status,
                failureReason, now());
        redemptions.put(key, row);
        return view(row);
    }

    public synchronized List<BenefitEligibilityView> eligibleBenefits(Long tenantId, String userId) {
        AccountRow account = requireAccount(tenantId, userId);
        int tierRank = tierRank(account.tierCode);
        return benefitRules().stream()
                .filter(rule -> tierRank >= tierRank(rule.minTierCode))
                .map(rule -> new BenefitEligibilityView(rule.benefitKey, rule.benefitName, rule.minTierCode, true,
                        "tier eligible"))
                .toList();
    }

    private AccountRow requireAccount(Long tenantId, String userId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedUserId = requireText(userId, "loyalty user id is required");
        AccountKey key = new AccountKey(scopedTenantId, scopedUserId);
        return accounts.computeIfAbsent(key, ignored -> {
            LocalDateTime now = now();
            return new AccountRow(++accountIds, scopedTenantId, scopedUserId, "M-" + scopedTenantId + "-"
                    + scopedUserId, BASIC, 0, 0, "ACTIVE", now);
        });
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static int positive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String tierForLifetime(int lifetimePoints) {
        if (lifetimePoints >= 10_000) {
            return PLATINUM;
        }
        if (lifetimePoints >= 1_000) {
            return GOLD;
        }
        if (lifetimePoints >= 100) {
            return SILVER;
        }
        return BASIC;
    }

    private static int tierRank(String tierCode) {
        return switch (tierCode == null ? BASIC : tierCode) {
            case SILVER -> 1;
            case GOLD -> 2;
            case PLATINUM -> 3;
            default -> 0;
        };
    }

    private static List<BenefitRule> benefitRules() {
        List<BenefitRule> rules = new ArrayList<>();
        rules.add(new BenefitRule("birthday_bonus", "Birthday Bonus", BASIC));
        rules.add(new BenefitRule("silver_coupon", "Silver Coupon", SILVER));
        rules.add(new BenefitRule("gold_shipping", "Gold Shipping", GOLD));
        return rules;
    }

    private static LoyaltyAccountView view(AccountRow row) {
        return new LoyaltyAccountView(row.accountId, row.tenantId, row.userId, row.memberNo, row.tierCode,
                row.pointsBalance, row.lifetimePoints, row.status);
    }

    private static RedemptionView view(RedemptionRow row) {
        return new RedemptionView(row.redemptionId, row.redemptionKey, row.rewardKey, row.pointsCost, row.status,
                row.failureReason, row.redeemedAt);
    }

    private record AccountKey(Long tenantId, String userId) {
    }

    private record TenantTransactionKey(Long tenantId, String key) {
    }

    private record BenefitRule(String benefitKey, String benefitName, String minTierCode) {
    }

    private static final class AccountRow {
        private final Long accountId;
        private final Long tenantId;
        private final String userId;
        private final String memberNo;
        private String tierCode;
        private int pointsBalance;
        private int lifetimePoints;
        private final String status;
        private LocalDateTime updatedAt;

        private AccountRow(Long accountId, Long tenantId, String userId, String memberNo, String tierCode,
                int pointsBalance, int lifetimePoints, String status, LocalDateTime updatedAt) {
            this.accountId = accountId;
            this.tenantId = tenantId;
            this.userId = userId;
            this.memberNo = memberNo;
            this.tierCode = tierCode;
            this.pointsBalance = pointsBalance;
            this.lifetimePoints = lifetimePoints;
            this.status = status;
            this.updatedAt = updatedAt;
        }
    }

    private record RedemptionRow(
            Long redemptionId,
            String redemptionKey,
            String rewardKey,
            int pointsCost,
            String status,
            String failureReason,
            LocalDateTime redeemedAt) {
    }
}
