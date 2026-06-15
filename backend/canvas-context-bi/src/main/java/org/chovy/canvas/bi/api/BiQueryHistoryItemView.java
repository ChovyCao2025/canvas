package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;

public record BiQueryHistoryItemView(
        Long id,
        String datasetKey,
        String username,
        int rowCount,
        long durationMs,
        String status,
        String sqlHash,
        String errorMessage,
        LocalDateTime createdAt) {
}
