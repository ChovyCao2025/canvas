package org.chovy.canvas.marketing.api;

import java.util.Map;

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
