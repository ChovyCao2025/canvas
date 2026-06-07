package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.util.Map;

public record GrowthRewardPoolCommand(
        String poolKey,
        String rewardType,
        String grantChannel,
        String couponTypeKey,
        String loyaltyRewardKey,
        String pointsType,
        String externalContractKey,
        String inventoryMode,
        Long totalInventory,
        Integer perUserLimit,
        Integer perReferralLimit,
        BigDecimal budgetAmount,
        String costCurrency,
        String status,
        Map<String, Object> metadata) {
}
