package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiQueryGovernanceSummaryView(
        int totalQueries,
        int slowQueries,
        int failedQueries,
        int cacheHits,
        long averageDurationMs,
        long timeoutPolicyMs,
        int datasetQuotaRows,
        List<Map<String, Object>> datasets,
        List<Map<String, Object>> slowAttributions) {

    public BiQueryGovernanceSummaryView {
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
        slowAttributions = slowAttributions == null ? List.of() : List.copyOf(slowAttributions);
    }
}
