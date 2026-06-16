package org.chovy.canvas.marketing.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 定义LoyaltyFacade的营销上下文访问契约。
 */
public interface LoyaltyFacade {

    /**
     * 执行account业务操作。
     */
    LoyaltyAccountView account(Long tenantId, String userId);

    /**
     * 执行earn业务操作。
     */
    LoyaltyAccountView earn(Long tenantId, String userId, EarnCommand command);

    /**
     * 执行redeem业务操作。
     */
    RedemptionView redeem(Long tenantId, String userId, RedemptionCommand command);

    /**
     * 执行eligibleBenefits业务操作。
     */
    List<BenefitEligibilityView> eligibleBenefits(Long tenantId, String userId);

    /**
     * 承载EarnCommand调用所需的输入参数。
     */
    static final class EarnCommand {

        /**
         * transactionKey 字段值。
         */
        private final String transactionKey;

        /**
         * points 字段值。
         */
        private final Integer points;

        /**
         * pointsType 字段值。
         */
        private final String pointsType;

        /**
         * sourceType 字段值。
         */
        private final String sourceType;

        /**
         * sourceId 字段值。
         */
        private final String sourceId;

        /**
         * 问题原因。
         */
        private final String reason;

        /**
         * 过期时间。
         */
        private final LocalDateTime expiresAt;

        /**
         * 创建EarnCommand实例。
         */
        public EarnCommand(String transactionKey, Integer points, String pointsType, String sourceType, String sourceId, String reason, LocalDateTime expiresAt) {
            this.transactionKey = transactionKey;
            this.points = points;
            this.pointsType = pointsType;
            this.sourceType = sourceType;
            this.sourceId = sourceId;
            this.reason = reason;
            this.expiresAt = expiresAt;
        }

        /**
         * 返回transactionKey 字段值。
         */
        public String transactionKey() {
            return transactionKey;
        }

        /**
         * 返回points 字段值。
         */
        public Integer points() {
            return points;
        }

        /**
         * 返回pointsType 字段值。
         */
        public String pointsType() {
            return pointsType;
        }

        /**
         * 返回sourceType 字段值。
         */
        public String sourceType() {
            return sourceType;
        }

        /**
         * 返回sourceId 字段值。
         */
        public String sourceId() {
            return sourceId;
        }

        /**
         * 返回问题原因。
         */
        public String reason() {
            return reason;
        }

        /**
         * 返回过期时间。
         */
        public LocalDateTime expiresAt() {
            return expiresAt;
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
            EarnCommand that = (EarnCommand) o;
            return                     Objects.equals(transactionKey, that.transactionKey) &&
                    Objects.equals(points, that.points) &&
                    Objects.equals(pointsType, that.pointsType) &&
                    Objects.equals(sourceType, that.sourceType) &&
                    Objects.equals(sourceId, that.sourceId) &&
                    Objects.equals(reason, that.reason) &&
                    Objects.equals(expiresAt, that.expiresAt);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(transactionKey, points, pointsType, sourceType, sourceId, reason, expiresAt);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "EarnCommand[transactionKey=" + transactionKey + ", points=" + points + ", pointsType=" + pointsType + ", sourceType=" + sourceType + ", sourceId=" + sourceId + ", reason=" + reason + ", expiresAt=" + expiresAt + "]";
        }
    }

    /**
     * 承载RedemptionCommand调用所需的输入参数。
     */
    static final class RedemptionCommand {

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
        private final Integer pointsCost;

        /**
         * 问题原因。
         */
        private final String reason;

        /**
         * 创建RedemptionCommand实例。
         */
        public RedemptionCommand(String redemptionKey, String rewardKey, Integer pointsCost, String reason) {
            this.redemptionKey = redemptionKey;
            this.rewardKey = rewardKey;
            this.pointsCost = pointsCost;
            this.reason = reason;
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
        public Integer pointsCost() {
            return pointsCost;
        }

        /**
         * 返回问题原因。
         */
        public String reason() {
            return reason;
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
            RedemptionCommand that = (RedemptionCommand) o;
            return                     Objects.equals(redemptionKey, that.redemptionKey) &&
                    Objects.equals(rewardKey, that.rewardKey) &&
                    Objects.equals(pointsCost, that.pointsCost) &&
                    Objects.equals(reason, that.reason);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(redemptionKey, rewardKey, pointsCost, reason);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "RedemptionCommand[redemptionKey=" + redemptionKey + ", rewardKey=" + rewardKey + ", pointsCost=" + pointsCost + ", reason=" + reason + "]";
        }
    }

    /**
     * 承载LoyaltyAccountView返回给调用方的只读视图。
     */
    static final class LoyaltyAccountView {

        /**
         * accountId 字段值。
         */
        private final Long accountId;

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * memberNo 字段值。
         */
        private final String memberNo;

        /**
         * tierCode 字段值。
         */
        private final String tierCode;

        /**
         * pointsBalance 字段值。
         */
        private final int pointsBalance;

        /**
         * lifetimePoints 字段值。
         */
        private final int lifetimePoints;

        /**
         * 当前业务状态。
         */
        private final String status;

        /**
         * 创建LoyaltyAccountView实例。
         */
        public LoyaltyAccountView(Long accountId, Long tenantId, String userId, String memberNo, String tierCode, int pointsBalance, int lifetimePoints, String status) {
            this.accountId = accountId;
            this.tenantId = tenantId;
            this.userId = userId;
            this.memberNo = memberNo;
            this.tierCode = tierCode;
            this.pointsBalance = pointsBalance;
            this.lifetimePoints = lifetimePoints;
            this.status = status;
        }

        /**
         * 返回accountId 字段值。
         */
        public Long accountId() {
            return accountId;
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
         * 返回memberNo 字段值。
         */
        public String memberNo() {
            return memberNo;
        }

        /**
         * 返回tierCode 字段值。
         */
        public String tierCode() {
            return tierCode;
        }

        /**
         * 返回pointsBalance 字段值。
         */
        public int pointsBalance() {
            return pointsBalance;
        }

        /**
         * 返回lifetimePoints 字段值。
         */
        public int lifetimePoints() {
            return lifetimePoints;
        }

        /**
         * 返回当前业务状态。
         */
        public String status() {
            return status;
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
            LoyaltyAccountView that = (LoyaltyAccountView) o;
            return                     Objects.equals(accountId, that.accountId) &&
                    Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(userId, that.userId) &&
                    Objects.equals(memberNo, that.memberNo) &&
                    Objects.equals(tierCode, that.tierCode) &&
                    pointsBalance == that.pointsBalance &&
                    lifetimePoints == that.lifetimePoints &&
                    Objects.equals(status, that.status);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(accountId, tenantId, userId, memberNo, tierCode, pointsBalance, lifetimePoints, status);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "LoyaltyAccountView[accountId=" + accountId + ", tenantId=" + tenantId + ", userId=" + userId + ", memberNo=" + memberNo + ", tierCode=" + tierCode + ", pointsBalance=" + pointsBalance + ", lifetimePoints=" + lifetimePoints + ", status=" + status + "]";
        }
    }

    /**
     * 承载RedemptionView返回给调用方的只读视图。
     */
    static final class RedemptionView {

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
         * 创建RedemptionView实例。
         */
        public RedemptionView(Long redemptionId, String redemptionKey, String rewardKey, int pointsCost, String status, String failureReason, LocalDateTime redeemedAt) {
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
            RedemptionView that = (RedemptionView) o;
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
            return "RedemptionView[redemptionId=" + redemptionId + ", redemptionKey=" + redemptionKey + ", rewardKey=" + rewardKey + ", pointsCost=" + pointsCost + ", status=" + status + ", failureReason=" + failureReason + ", redeemedAt=" + redeemedAt + "]";
        }
    }

    /**
     * 承载BenefitEligibilityView返回给调用方的只读视图。
     */
    static final class BenefitEligibilityView {

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
         * eligible 字段值。
         */
        private final boolean eligible;

        /**
         * 问题原因。
         */
        private final String reason;

        /**
         * 创建BenefitEligibilityView实例。
         */
        public BenefitEligibilityView(String benefitKey, String benefitName, String minTierCode, boolean eligible, String reason) {
            this.benefitKey = benefitKey;
            this.benefitName = benefitName;
            this.minTierCode = minTierCode;
            this.eligible = eligible;
            this.reason = reason;
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
         * 返回eligible 字段值。
         */
        public boolean eligible() {
            return eligible;
        }

        /**
         * 返回问题原因。
         */
        public String reason() {
            return reason;
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
            BenefitEligibilityView that = (BenefitEligibilityView) o;
            return                     Objects.equals(benefitKey, that.benefitKey) &&
                    Objects.equals(benefitName, that.benefitName) &&
                    Objects.equals(minTierCode, that.minTierCode) &&
                    eligible == that.eligible &&
                    Objects.equals(reason, that.reason);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(benefitKey, benefitName, minTierCode, eligible, reason);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "BenefitEligibilityView[benefitKey=" + benefitKey + ", benefitName=" + benefitName + ", minTierCode=" + minTierCode + ", eligible=" + eligible + ", reason=" + reason + "]";
        }
    }
}
