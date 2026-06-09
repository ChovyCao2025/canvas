package org.chovy.canvas.domain.ai;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * AiDecisionRunView 承载 domain.ai 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param modelKey modelKey 字段。
 * @param modelVersion modelVersion 字段。
 * @param decisionScope decisionScope 字段。
 * @param runDate runDate 字段。
 * @param status status 字段。
 * @param requestedCount requestedCount 字段。
 * @param processedCount processedCount 字段。
 * @param skippedCount skippedCount 字段。
 * @param failedCount failedCount 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param startedAt startedAt 字段。
 * @param finishedAt finishedAt 字段。
 * @param errorMessage errorMessage 字段。
 */
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
