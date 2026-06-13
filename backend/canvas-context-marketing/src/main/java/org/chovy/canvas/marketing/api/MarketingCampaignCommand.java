package org.chovy.canvas.marketing.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record MarketingCampaignCommand(
        String campaignKey,
        String campaignName,
        String objective,
        String status,
        String primaryChannel,
        String ownerTeam,
        LocalDateTime startAt,
        LocalDateTime endAt,
        BigDecimal budgetAmount,
        String currency,
        Map<String, Object> brief) {
}
