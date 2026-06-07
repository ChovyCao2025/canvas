package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record CreatorCampaignCommand(
        String campaignKey,
        String campaignName,
        String objective,
        BigDecimal budgetAmount,
        String currency,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        Map<String, Object> metadata) {

    public CreatorCampaignCommand {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
