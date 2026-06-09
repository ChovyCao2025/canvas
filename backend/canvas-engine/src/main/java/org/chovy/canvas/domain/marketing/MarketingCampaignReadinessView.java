package org.chovy.canvas.domain.marketing;

import java.util.List;

/**
 * MarketingCampaignReadinessView 承载 domain.marketing 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param campaignId campaignId 字段。
 * @param campaignKey campaignKey 字段。
 * @param campaignName campaignName 字段。
 * @param generatedAt generatedAt 字段。
 * @param status status 字段。
 * @param productionReady productionReady 字段。
 * @param requiredLinkCount requiredLinkCount 字段。
 * @param activeRequiredLinkCount activeRequiredLinkCount 字段。
 * @param blockerCount blockerCount 字段。
 * @param warningCount warningCount 字段。
 * @param blockers blockers 字段。
 * @param warnings warnings 字段。
 * @param links links 字段。
 */
public record MarketingCampaignReadinessView(
        Long tenantId,
        Long campaignId,
        String campaignKey,
        String campaignName,
        String generatedAt,
        String status,
        boolean productionReady,
        int requiredLinkCount,
        int activeRequiredLinkCount,
        int blockerCount,
        int warningCount,
        List<MarketingCampaignReadinessFinding> blockers,
        List<MarketingCampaignReadinessFinding> warnings,
        List<MarketingCampaignLinkView> links) {
}
