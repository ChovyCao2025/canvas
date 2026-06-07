package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

public record BiDatasetExtractCapacitySummaryView(
        String datasetKey,
        Boolean enabled,
        String accelerationMode,
        String refreshMode,
        String materializedTable,
        String lastStatus,
        LocalDateTime lastRefreshedAt,
        Integer retentionLimit,
        Integer successfulRuns,
        Integer failedRuns,
        Integer activeTables,
        Integer droppedTables,
        Integer staleTables,
        Long retainedRows,
        Long latestRowCount,
        Long latestDurationMs) {
}
