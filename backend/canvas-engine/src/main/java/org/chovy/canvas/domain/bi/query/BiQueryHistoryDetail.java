package org.chovy.canvas.domain.bi.query;

import java.time.LocalDateTime;

public record BiQueryHistoryDetail(
        Long id,
        String datasetKey,
        String username,
        BiQueryRequest request,
        int rowCount,
        long durationMs,
        String status,
        String sqlHash,
        String errorMessage,
        LocalDateTime createdAt
) {
}
