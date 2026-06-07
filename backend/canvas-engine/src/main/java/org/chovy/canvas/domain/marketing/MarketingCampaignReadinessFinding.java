package org.chovy.canvas.domain.marketing;

public record MarketingCampaignReadinessFinding(
        String severity,
        String itemType,
        String itemKey,
        String title,
        String reason,
        String route) {
}
