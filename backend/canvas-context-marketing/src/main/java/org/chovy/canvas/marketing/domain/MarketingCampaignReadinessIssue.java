package org.chovy.canvas.marketing.domain;

public record MarketingCampaignReadinessIssue(
        String severity,
        String itemType,
        String itemKey,
        String title,
        String reason,
        String route) {
}
