package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record CreatorCampaignView(
        Long id,
        Long tenantId,
        String campaignKey,
        String campaignName,
        String objective,
        BigDecimal budgetAmount,
        String currency,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public CreatorCampaignView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
