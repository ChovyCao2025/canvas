package org.chovy.canvas.marketing.domain;

import java.util.List;

public record MarketingCampaignReadinessReport(
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
        List<MarketingCampaignReadinessIssue> blockers,
        List<MarketingCampaignReadinessIssue> warnings,
        List<MarketingCampaignLink> links) {

    public MarketingCampaignReadinessReport {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        links = links == null ? List.of() : List.copyOf(links);
    }
}
