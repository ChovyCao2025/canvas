package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record GrowthRewardPoolView(
        Long id,
        Long tenantId,
        Long activityId,
        String poolKey,
        String rewardType,
        String grantChannel,
        String couponTypeKey,
        String loyaltyRewardKey,
        String pointsType,
        String externalContractKey,
        String inventoryMode,
        Long totalInventory,
        Long reservedInventory,
        Long grantedInventory,
        Integer perUserLimit,
        Integer perReferralLimit,
        BigDecimal budgetAmount,
        BigDecimal reservedAmount,
        BigDecimal grantedAmount,
        String costCurrency,
        String status,
        boolean inventoryLow,
        Map<String, Object> metadata,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
