package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

public record BiDatasetAccelerationSchedulerItem(
        String datasetKey,
        String status,
        String reason,
        Long refreshRunId,
        Long rowCount,
        Long durationMs,
        String materializedTable,
        LocalDateTime startedAt,
        LocalDateTime finishedAt) {
}
