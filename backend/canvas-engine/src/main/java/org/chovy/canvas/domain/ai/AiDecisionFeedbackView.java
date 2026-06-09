package org.chovy.canvas.domain.ai;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * AiDecisionFeedbackView 承载 domain.ai 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param recommendationId recommendationId 字段。
 * @param feedbackType feedbackType 字段。
 * @param outcomeValue outcomeValue 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param occurredAt occurredAt 字段。
 */
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
