package org.chovy.canvas.domain.programmatic;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record ProgrammaticDspCampaignCommand(
        String campaignKey,
        String campaignName,
        String objective,
        BigDecimal budgetAmount,
        String currency,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        Map<String, Object> metadata) {
}
