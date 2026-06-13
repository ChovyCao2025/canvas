package org.chovy.canvas.marketing.api;

import java.time.LocalDateTime;
import java.util.Map;

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
