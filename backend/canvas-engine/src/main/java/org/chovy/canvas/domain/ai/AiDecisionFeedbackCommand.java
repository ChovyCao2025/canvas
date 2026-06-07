package org.chovy.canvas.domain.ai;

import java.math.BigDecimal;
import java.util.Map;

public record AiDecisionFeedbackCommand(
        String feedbackType,
        BigDecimal outcomeValue,
        Map<String, Object> metadata) {

    public AiDecisionFeedbackCommand {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
