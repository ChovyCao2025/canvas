package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * MarketingCampaignView 承载 domain.marketing 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param campaignKey campaignKey 字段。
 * @param campaignName campaignName 字段。
 * @param objective objective 字段。
 * @param status status 字段。
 * @param primaryChannel primaryChannel 字段。
 * @param ownerTeam ownerTeam 字段。
 * @param startAt startAt 字段。
 * @param endAt endAt 字段。
 * @param budgetAmount budgetAmount 字段。
 * @param currency currency 字段。
 * @param brief brief 字段。
 * @param createdBy createdBy 字段。
 * @param updatedBy updatedBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record MarketingCampaignView(
        Long id,
        Long tenantId,
        String campaignKey,
        String campaignName,
        String objective,
        String status,
        String primaryChannel,
        String ownerTeam,
        LocalDateTime startAt,
        LocalDateTime endAt,
        BigDecimal budgetAmount,
        String currency,
        Map<String, Object> brief,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
