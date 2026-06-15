package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;

public record BiQueryHistoryDetailView(
        Long id,
        String datasetKey,
        String username,
        BiQueryCommand request,
        int rowCount,
        long durationMs,
        String status,
        String sqlHash,
        String errorMessage,
        LocalDateTime createdAt) {
}
