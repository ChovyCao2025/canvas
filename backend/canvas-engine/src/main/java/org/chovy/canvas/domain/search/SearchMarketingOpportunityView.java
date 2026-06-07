package org.chovy.canvas.domain.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record SearchMarketingOpportunityView(
        Long id,
        Long tenantId,
        Long sourceId,
        Long keywordId,
        String channel,
        String opportunityType,
        LocalDate snapshotDate,
        String severity,
        String status,
        String recommendation,
        BigDecimal impactScore,
        Map<String, Object> evidence,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public SearchMarketingOpportunityView {
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
