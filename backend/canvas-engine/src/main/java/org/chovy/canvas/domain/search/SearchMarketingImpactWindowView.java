package org.chovy.canvas.domain.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record SearchMarketingImpactWindowView(
        Long id,
        Long tenantId,
        Long opportunityId,
        Long mutationId,
        Long sourceId,
        Long keywordId,
        String pageUrlHash,
        LocalDate baselineStartDate,
        LocalDate baselineEndDate,
        LocalDate postStartDate,
        LocalDate postEndDate,
        String status,
        String decision,
        BigDecimal confidence,
        Map<String, Object> metricDeltas,
        Map<String, Object> evidence,
        LocalDateTime dueAt,
        LocalDateTime evaluatedAt,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public SearchMarketingImpactWindowView {
        metricDeltas = metricDeltas == null ? Map.of() : Map.copyOf(metricDeltas);
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
