package org.chovy.canvas.domain.ai;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * AiDecisionRecommendationView 承载 domain.ai 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param runId runId 字段。
 * @param userId userId 字段。
 * @param modelKey modelKey 字段。
 * @param modelVersion modelVersion 字段。
 * @param decisionScope decisionScope 字段。
 * @param decisionType decisionType 字段。
 * @param decisionKey decisionKey 字段。
 * @param actionKey actionKey 字段。
 * @param offerKey offerKey 字段。
 * @param channel channel 字段。
 * @param score score 字段。
 * @param confidence confidence 字段。
 * @param recommendationRank recommendationRank 字段。
 * @param budgetCost budgetCost 字段。
 * @param eligibilityStatus eligibilityStatus 字段。
 * @param fallbackReason fallbackReason 字段。
 * @param features features 字段。
 * @param explanation explanation 字段。
 * @param createdAt createdAt 字段。
 */
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
