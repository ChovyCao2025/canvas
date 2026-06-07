package org.chovy.canvas.domain.search;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

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
