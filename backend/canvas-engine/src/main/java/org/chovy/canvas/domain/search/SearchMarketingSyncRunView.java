package org.chovy.canvas.domain.search;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SearchMarketingSyncRunView 承载 domain.search 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param runType runType 字段。
 * @param provider provider 字段。
 * @param channel channel 字段。
 * @param idempotencyKey idempotencyKey 字段。
 * @param windowStart windowStart 字段。
 * @param windowEnd windowEnd 字段。
 * @param cursorValue cursorValue 字段。
 * @param status status 字段。
 * @param retryable retryable 字段。
 * @param requestedCount requestedCount 字段。
 * @param successCount successCount 字段。
 * @param failedCount failedCount 字段。
 * @param providerRequestId providerRequestId 字段。
 * @param errorCode errorCode 字段。
 * @param errorMessage errorMessage 字段。
 * @param evidence evidence 字段。
 * @param createdBy createdBy 字段。
 * @param startedAt startedAt 字段。
 * @param finishedAt finishedAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record SearchMarketingSyncRunView(
        Long id,
        Long tenantId,
        Long sourceId,
        String runType,
        String provider,
        String channel,
        String idempotencyKey,
        LocalDate windowStart,
        LocalDate windowEnd,
        String cursorValue,
        String status,
        boolean retryable,
        long requestedCount,
        long successCount,
        long failedCount,
        String providerRequestId,
        String errorCode,
        String errorMessage,
        Map<String, Object> evidence,
        String createdBy,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime updatedAt) {

    public SearchMarketingSyncRunView {
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
