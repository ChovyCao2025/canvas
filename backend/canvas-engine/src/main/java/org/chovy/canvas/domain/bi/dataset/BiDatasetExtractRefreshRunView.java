package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

public record BiDatasetExtractRefreshRunView(
        Long id,
        String datasetKey,
        String status,
        Long rowCount,
        Long durationMs,
        String materializedTable,
        String requestedBy,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String errorMessage) {
}
