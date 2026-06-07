package org.chovy.canvas.domain.marketing;

import java.util.List;

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
