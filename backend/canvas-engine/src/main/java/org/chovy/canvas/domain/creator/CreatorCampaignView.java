package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * CreatorCampaignView 承载 domain.creator 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param campaignKey campaignKey 字段。
 * @param campaignName campaignName 字段。
 * @param objective objective 字段。
 * @param budgetAmount budgetAmount 字段。
 * @param currency currency 字段。
 * @param startAt startAt 字段。
 * @param endAt endAt 字段。
 * @param status status 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record CreatorCampaignView(
        Long id,
        Long tenantId,
        String campaignKey,
        String campaignName,
        String objective,
        BigDecimal budgetAmount,
        String currency,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public CreatorCampaignView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
