package org.chovy.canvas.domain.ai;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record AiDecisionRecommendationView(
        Long id,
        Long tenantId,
        Long runId,
        String userId,
        String modelKey,
        String modelVersion,
        String decisionScope,
        String decisionType,
        String decisionKey,
        String actionKey,
        String offerKey,
        String channel,
        BigDecimal score,
        BigDecimal confidence,
        Integer recommendationRank,
        BigDecimal budgetCost,
        String eligibilityStatus,
        String fallbackReason,
        Map<String, Object> features,
        Map<String, Object> explanation,
        LocalDateTime createdAt) {

    public AiDecisionRecommendationView {
        features = features == null ? Map.of() : Map.copyOf(features);
        explanation = explanation == null ? Map.of() : Map.copyOf(explanation);
    }
}
