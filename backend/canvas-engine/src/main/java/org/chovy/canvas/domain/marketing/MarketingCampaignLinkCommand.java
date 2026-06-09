package org.chovy.canvas.domain.marketing;

import java.util.Map;

/**
 * MarketingCampaignLinkCommand 承载 domain.marketing 场景中的不可变数据快照。
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
 */
public record MarketingCampaignLinkCommand(
        Long campaignId,
        String resourceType,
        Long resourceId,
        String resourceKey,
        String resourceName,
        String resourceRoute,
        String dependencyRole,
        String linkStatus,
        Boolean requiredForLaunch,
        Map<String, Object> metadata) {
}
