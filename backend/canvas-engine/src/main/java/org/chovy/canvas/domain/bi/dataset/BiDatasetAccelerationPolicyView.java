package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;
import java.util.List;

public record BiDatasetAccelerationPolicyView(
        String datasetKey,
        Boolean enabled,
        String accelerationMode,
        String refreshMode,
        Long refreshIntervalMinutes,
        Long ttlSeconds,
        Long maxRows,
        String cronExpression,
        String materializedTable,
        String lastStatus,
        Long lastRunId,
        LocalDateTime lastRefreshedAt,
        List<BiDatasetExtractRefreshRunView> recentRuns) {

    public BiDatasetAccelerationPolicyView {
        recentRuns = recentRuns == null ? List.of() : List.copyOf(recentRuns);
    }
}
