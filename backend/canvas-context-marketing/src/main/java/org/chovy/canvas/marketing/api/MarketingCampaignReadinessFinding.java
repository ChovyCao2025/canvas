package org.chovy.canvas.marketing.api;

public record MarketingCampaignReadinessFinding(
        String severity,
        String itemType,
        String itemKey,
        String title,
        String reason,
        String route) {
}
