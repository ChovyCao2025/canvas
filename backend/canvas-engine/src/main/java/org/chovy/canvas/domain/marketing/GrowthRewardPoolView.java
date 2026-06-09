package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * GrowthRewardPoolView 承载 domain.marketing 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param activityId activityId 字段。
 * @param poolKey poolKey 字段。
 * @param rewardType rewardType 字段。
 * @param grantChannel grantChannel 字段。
 * @param couponTypeKey couponTypeKey 字段。
 * @param loyaltyRewardKey loyaltyRewardKey 字段。
 * @param pointsType pointsType 字段。
 * @param externalContractKey externalContractKey 字段。
 * @param inventoryMode inventoryMode 字段。
 * @param totalInventory totalInventory 字段。
 * @param reservedInventory reservedInventory 字段。
 * @param grantedInventory grantedInventory 字段。
 * @param perUserLimit perUserLimit 字段。
 * @param perReferralLimit perReferralLimit 字段。
 * @param budgetAmount budgetAmount 字段。
 * @param reservedAmount reservedAmount 字段。
 * @param grantedAmount grantedAmount 字段。
 * @param costCurrency costCurrency 字段。
 * @param status status 字段。
 * @param inventoryLow inventoryLow 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param updatedBy updatedBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
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
