package org.chovy.canvas.domain.bi.query;

public record BiQueryHistoryEntry(
        Long tenantId,
        String username,
        BiQueryRequest request,
        String sqlHash,
        int rowCount,
        long durationMs,
        String status,
        String errorMessage
) {
}
