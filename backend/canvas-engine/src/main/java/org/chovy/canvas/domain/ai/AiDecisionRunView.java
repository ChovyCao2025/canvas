package org.chovy.canvas.domain.ai;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record AiDecisionRunView(
        Long id,
        Long tenantId,
        String modelKey,
        String modelVersion,
        String decisionScope,
        LocalDate runDate,
        String status,
        Integer requestedCount,
        Integer processedCount,
        Integer skippedCount,
        Integer failedCount,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String errorMessage) {

    public AiDecisionRunView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
