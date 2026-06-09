package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.util.Map;

/**
 * GrowthRewardPoolCommand 承载 domain.marketing 场景中的不可变数据快照。
 * @param poolKey poolKey 字段。
 * @param rewardType rewardType 字段。
 * @param grantChannel grantChannel 字段。
 * @param couponTypeKey couponTypeKey 字段。
 * @param loyaltyRewardKey loyaltyRewardKey 字段。
 * @param pointsType pointsType 字段。
 * @param externalContractKey externalContractKey 字段。
 * @param inventoryMode inventoryMode 字段。
 * @param totalInventory totalInventory 字段。
 * @param perUserLimit perUserLimit 字段。
 * @param perReferralLimit perReferralLimit 字段。
 * @param budgetAmount budgetAmount 字段。
 * @param costCurrency costCurrency 字段。
 * @param status status 字段。
 * @param metadata metadata 字段。
 */
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
