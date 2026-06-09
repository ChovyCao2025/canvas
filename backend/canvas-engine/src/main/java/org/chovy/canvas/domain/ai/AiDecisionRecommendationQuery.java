package org.chovy.canvas.domain.ai;

/**
 * AiDecisionRecommendationQuery 承载 domain.ai 场景中的不可变数据快照。
 * @param runId runId 字段。
 * @param decisionType decisionType 字段。
 * @param eligibilityStatus eligibilityStatus 字段。
 * @param limit limit 字段。
 */
public record AiDecisionRecommendationQuery(
        Long runId,
        String decisionType,
        String eligibilityStatus,
        int limit) {
}
