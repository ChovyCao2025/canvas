package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MarketingCampaignLinkView 承载 domain.marketing 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param campaignId campaignId 字段。
 * @param resourceType resourceType 字段。
 * @param resourceId resourceId 字段。
 * @param resourceKey resourceKey 字段。
 * @param resourceName resourceName 字段。
 * @param resourceRoute resourceRoute 字段。
 * @param dependencyRole dependencyRole 字段。
 * @param linkStatus linkStatus 字段。
 * @param requiredForLaunch requiredForLaunch 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param updatedBy updatedBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record MarketingCampaignLinkView(
        Long id,
        Long tenantId,
        Long campaignId,
        String resourceType,
        Long resourceId,
        String resourceKey,
        String resourceName,
        String resourceRoute,
        String dependencyRole,
        String linkStatus,
        boolean requiredForLaunch,
        Map<String, Object> metadata,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
