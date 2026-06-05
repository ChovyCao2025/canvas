package org.chovy.canvas.domain.bi.query;

import java.time.LocalDateTime;

public record BiQueryHistoryItem(
        Long id,
        String datasetKey,
        String username,
        int rowCount,
        long durationMs,
        String status,
        String sqlHash,
        String errorMessage,
        LocalDateTime createdAt
) {
}
