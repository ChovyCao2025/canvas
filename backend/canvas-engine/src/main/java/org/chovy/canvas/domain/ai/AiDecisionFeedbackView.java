package org.chovy.canvas.domain.ai;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record AiDecisionFeedbackView(
        Long id,
        Long tenantId,
        Long recommendationId,
        String feedbackType,
        BigDecimal outcomeValue,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime occurredAt) {

    public AiDecisionFeedbackView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
