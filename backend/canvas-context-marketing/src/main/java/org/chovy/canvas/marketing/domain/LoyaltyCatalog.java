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
import java.util.Objects;

/**
 * 维护Loyalty相关的内存业务目录。
 */
public class LoyaltyCatalog {

    /**
     * 保存BASIC字段值。
     */
    private static final String BASIC = "BASIC";

    /**
     * 保存SILVER字段值。
     */
    private static final String SILVER = "SILVER";

    /**
     * 保存GOLD字段值。
     */
    private static final String GOLD = "GOLD";

    /**
     * 保存PLATINUM字段值。
     */
    private static final String PLATINUM = "PLATINUM";

    /**
     * 用于生成确定性业务时间的时钟。
     */
    private final Clock clock;
    private final Map<AccountKey, AccountRow> accounts = new LinkedHashMap<>();
    private final Map<TenantTransactionKey, String> earnedTransactionKeys = new LinkedHashMap<>();
    private final Map<TenantTransactionKey, RedemptionRow> redemptions = new LinkedHashMap<>();

    /**
     * 保存accountIds字段值。
     */
    private long accountIds;

    /**
     * 保存redemptionIds字段值。
     */
    private long redemptionIds;

    /**
     * 创建LoyaltyCatalog实例。
     */
    public LoyaltyCatalog(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行account业务操作。
     */
    public synchronized LoyaltyAccountView account(Long tenantId, String userId) {
        return view(requireAccount(tenantId, userId));
    }

    /**
     * 执行earn业务操作。
     */
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

    /**
     * 执行redeem业务操作。
     */
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

    /**
     * 执行eligibleBenefits业务操作。
     */
    public synchronized List<BenefitEligibilityView> eligibleBenefits(Long tenantId, String userId) {
        AccountRow account = requireAccount(tenantId, userId);
        int tierRank = tierRank(account.tierCode);
        return benefitRules().stream()
                .filter(rule -> tierRank >= tierRank(rule.minTierCode))
                .map(rule -> new BenefitEligibilityView(rule.benefitKey, rule.benefitName, rule.minTierCode, true,
                        "tier eligible"))
                .toList();
    }

    /**
     * 校验并返回account必填值。
     */
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

    /**
     * 执行now业务操作。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 规范化tenant输入值。
     */
    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 校验并返回text必填值。
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 执行positive业务操作。
     */
    private static int positive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * 执行tierForLifetime业务操作。
     */
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

    /**
     * 执行tierRank业务操作。
     */
    private static int tierRank(String tierCode) {
        return switch (tierCode == null ? BASIC : tierCode) {
            case SILVER -> 1;
            case GOLD -> 2;
            case PLATINUM -> 3;
            default -> 0;
        };
    }

    /**
     * 执行benefitRules业务操作。
     */
    private static List<BenefitRule> benefitRules() {
        List<BenefitRule> rules = new ArrayList<>();
        rules.add(new BenefitRule("birthday_bonus", "Birthday Bonus", BASIC));
        rules.add(new BenefitRule("silver_coupon", "Silver Coupon", SILVER));
        rules.add(new BenefitRule("gold_shipping", "Gold Shipping", GOLD));
        return rules;
    }

    /**
     * 执行view业务操作。
     */
    private static LoyaltyAccountView view(AccountRow row) {
        return new LoyaltyAccountView(row.accountId, row.tenantId, row.userId, row.memberNo, row.tierCode,
                row.pointsBalance, row.lifetimePoints, row.status);
    }

    /**
     * 执行view业务操作。
     */
    private static RedemptionView view(RedemptionRow row) {
        return new RedemptionView(row.redemptionId, row.redemptionKey, row.rewardKey, row.pointsCost, row.status,
                row.failureReason, row.redeemedAt);
    }

    /**
     * 表示AccountKey使用的稳定匹配键。
     */
    private static final class AccountKey {

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * 创建AccountKey实例。
         */
        public AccountKey(Long tenantId, String userId) {
            this.tenantId = tenantId;
            this.userId = userId;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AccountKey that = (AccountKey) o;
            return                     Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(userId, that.userId);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, userId);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "AccountKey[tenantId=" + tenantId + ", userId=" + userId + "]";
        }
    }

    /**
     * 表示TenantTransactionKey使用的稳定匹配键。
     */
    private static final class TenantTransactionKey {

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * 选项键。
         */
        private final String key;

        /**
         * 创建TenantTransactionKey实例。
         */
        public TenantTransactionKey(Long tenantId, String key) {
            this.tenantId = tenantId;
            this.key = key;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回选项键。
         */
        public String key() {
            return key;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TenantTransactionKey that = (TenantTransactionKey) o;
            return                     Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(key, that.key);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, key);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "TenantTransactionKey[tenantId=" + tenantId + ", key=" + key + "]";
        }
    }

    /**
     * 表示BenefitRule的数据结构。
     */
    private static final class BenefitRule {

        /**
         * benefitKey 字段值。
         */
        private final String benefitKey;

        /**
         * benefitName 字段值。
         */
        private final String benefitName;

        /**
         * minTierCode 字段值。
         */
        private final String minTierCode;

        /**
         * 创建BenefitRule实例。
         */
        public BenefitRule(String benefitKey, String benefitName, String minTierCode) {
            this.benefitKey = benefitKey;
            this.benefitName = benefitName;
            this.minTierCode = minTierCode;
        }

        /**
         * 返回benefitKey 字段值。
         */
        public String benefitKey() {
            return benefitKey;
        }

        /**
         * 返回benefitName 字段值。
         */
        public String benefitName() {
            return benefitName;
        }

        /**
         * 返回minTierCode 字段值。
         */
        public String minTierCode() {
            return minTierCode;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BenefitRule that = (BenefitRule) o;
            return                     Objects.equals(benefitKey, that.benefitKey) &&
                    Objects.equals(benefitName, that.benefitName) &&
                    Objects.equals(minTierCode, that.minTierCode);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(benefitKey, benefitName, minTierCode);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "BenefitRule[benefitKey=" + benefitKey + ", benefitName=" + benefitName + ", minTierCode=" + minTierCode + "]";
        }
    }

    /**
     * 提供AccountRow的业务能力。
     */
    private static final class AccountRow {
        /**
         * 保存accountId字段值。
         */
        private final Long accountId;

        /**
         * 保存tenantId字段值。
         */
        private final Long tenantId;

        /**
         * 保存userId字段值。
         */
        private final String userId;

        /**
         * 保存memberNo字段值。
         */
        private final String memberNo;

        /**
         * 保存tierCode字段值。
         */
        private String tierCode;

        /**
         * 保存pointsBalance字段值。
         */
        private int pointsBalance;

        /**
         * 保存lifetimePoints字段值。
         */
        private int lifetimePoints;

        /**
         * 保存status字段值。
         */
        private final String status;

        /**
         * 保存updatedAt字段值。
         */
        private LocalDateTime updatedAt;

        /**
         * 创建AccountRow实例。
         */
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

    /**
     * 保存RedemptionRow的内存行数据。
     */
    private static final class RedemptionRow {

        /**
         * redemptionId 字段值。
         */
        private final Long redemptionId;

        /**
         * redemptionKey 字段值。
         */
        private final String redemptionKey;

        /**
         * rewardKey 字段值。
         */
        private final String rewardKey;

        /**
         * pointsCost 字段值。
         */
        private final int pointsCost;

        /**
         * 当前业务状态。
         */
        private final String status;

        /**
         * failureReason 字段值。
         */
        private final String failureReason;

        /**
         * redeemedAt 字段值。
         */
        private final LocalDateTime redeemedAt;

        /**
         * 创建RedemptionRow实例。
         */
        public RedemptionRow(Long redemptionId, String redemptionKey, String rewardKey, int pointsCost, String status, String failureReason, LocalDateTime redeemedAt) {
            this.redemptionId = redemptionId;
            this.redemptionKey = redemptionKey;
            this.rewardKey = rewardKey;
            this.pointsCost = pointsCost;
            this.status = status;
            this.failureReason = failureReason;
            this.redeemedAt = redeemedAt;
        }

        /**
         * 返回redemptionId 字段值。
         */
        public Long redemptionId() {
            return redemptionId;
        }

        /**
         * 返回redemptionKey 字段值。
         */
        public String redemptionKey() {
            return redemptionKey;
        }

        /**
         * 返回rewardKey 字段值。
         */
        public String rewardKey() {
            return rewardKey;
        }

        /**
         * 返回pointsCost 字段值。
         */
        public int pointsCost() {
            return pointsCost;
        }

        /**
         * 返回当前业务状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回failureReason 字段值。
         */
        public String failureReason() {
            return failureReason;
        }

        /**
         * 返回redeemedAt 字段值。
         */
        public LocalDateTime redeemedAt() {
            return redeemedAt;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RedemptionRow that = (RedemptionRow) o;
            return                     Objects.equals(redemptionId, that.redemptionId) &&
                    Objects.equals(redemptionKey, that.redemptionKey) &&
                    Objects.equals(rewardKey, that.rewardKey) &&
                    pointsCost == that.pointsCost &&
                    Objects.equals(status, that.status) &&
                    Objects.equals(failureReason, that.failureReason) &&
                    Objects.equals(redeemedAt, that.redeemedAt);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(redemptionId, redemptionKey, rewardKey, pointsCost, status, failureReason, redeemedAt);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "RedemptionRow[redemptionId=" + redemptionId + ", redemptionKey=" + redemptionKey + ", rewardKey=" + rewardKey + ", pointsCost=" + pointsCost + ", status=" + status + ", failureReason=" + failureReason + ", redeemedAt=" + redeemedAt + "]";
        }
    }
}
