package org.chovy.canvas.marketing.api;

import java.time.LocalDateTime;
import java.util.List;

public interface LoyaltyFacade {

    LoyaltyAccountView account(Long tenantId, String userId);

    LoyaltyAccountView earn(Long tenantId, String userId, EarnCommand command);

    RedemptionView redeem(Long tenantId, String userId, RedemptionCommand command);

    List<BenefitEligibilityView> eligibleBenefits(Long tenantId, String userId);

    record EarnCommand(
            String transactionKey,
            Integer points,
            String pointsType,
            String sourceType,
            String sourceId,
            String reason,
            LocalDateTime expiresAt) {
    }

    record RedemptionCommand(String redemptionKey, String rewardKey, Integer pointsCost, String reason) {
    }

    record LoyaltyAccountView(
            Long accountId,
            Long tenantId,
            String userId,
            String memberNo,
            String tierCode,
            int pointsBalance,
            int lifetimePoints,
            String status) {
    }

    record RedemptionView(
            Long redemptionId,
            String redemptionKey,
            String rewardKey,
            int pointsCost,
            String status,
            String failureReason,
            LocalDateTime redeemedAt) {
    }

    record BenefitEligibilityView(
            String benefitKey,
            String benefitName,
            String minTierCode,
            boolean eligible,
            String reason) {
    }
}
