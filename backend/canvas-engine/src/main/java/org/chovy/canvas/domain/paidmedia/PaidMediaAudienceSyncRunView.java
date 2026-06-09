package org.chovy.canvas.domain.paidmedia;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * PaidMediaAudienceSyncRunView 承载 domain.paidmedia 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param destinationId destinationId 字段。
 * @param audienceId audienceId 字段。
 * @param provider provider 字段。
 * @param status status 字段。
 * @param requestedCount requestedCount 字段。
 * @param eligibleCount eligibleCount 字段。
 * @param skippedCount skippedCount 字段。
 * @param failedCount failedCount 字段。
 * @param externalOperationId externalOperationId 字段。
 * @param errorMessage errorMessage 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param startedAt startedAt 字段。
 * @param finishedAt finishedAt 字段。
 */
public record PaidMediaAudienceSyncRunView(
        Long id,
        Long tenantId,
        Long destinationId,
        Long audienceId,
        String provider,
        String status,
        Integer requestedCount,
        Integer eligibleCount,
        Integer skippedCount,
        Integer failedCount,
        String externalOperationId,
        String errorMessage,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime startedAt,
        LocalDateTime finishedAt) {

    public PaidMediaAudienceSyncRunView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
