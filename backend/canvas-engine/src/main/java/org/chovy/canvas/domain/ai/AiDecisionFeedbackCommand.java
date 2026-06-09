package org.chovy.canvas.domain.ai;

import java.math.BigDecimal;
import java.util.Map;

/**
 * AiDecisionFeedbackCommand 承载 domain.ai 场景中的不可变数据快照。
 * @param feedbackType feedbackType 字段。
 * @param outcomeValue outcomeValue 字段。
 * @param metadata metadata 字段。
 */
public record AiDecisionFeedbackCommand(
        String feedbackType,
        BigDecimal outcomeValue,
        Map<String, Object> metadata) {

    public AiDecisionFeedbackCommand {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
