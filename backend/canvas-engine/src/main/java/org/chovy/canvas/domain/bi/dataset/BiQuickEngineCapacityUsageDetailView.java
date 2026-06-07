package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

public record BiQuickEngineCapacityUsageDetailView(
        String type,
        String resourceKey,
        long usedRows,
        int activeTables,
        Long latestRunId,
        LocalDateTime latestFinishedAt,
        Long latestRowCount,
        String owner) {
}
