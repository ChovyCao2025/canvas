package org.chovy.canvas.domain.ai;

public record AiDecisionRecommendationQuery(
        Long runId,
        String decisionType,
        String eligibilityStatus,
        int limit) {
}
