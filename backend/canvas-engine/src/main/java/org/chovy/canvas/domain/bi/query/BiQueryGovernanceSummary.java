package org.chovy.canvas.domain.bi.query;

import java.util.List;

public record BiQueryGovernanceSummary(
        int totalQueries,
        int slowQueries,
        int failedQueries,
        int cacheHits,
        long averageDurationMs,
        long timeoutPolicyMs,
        int datasetQuotaRows,
        List<DatasetQueryStats> datasets,
        List<SlowQueryAttribution> slowAttributions
) {
    public BiQueryGovernanceSummary(int totalQueries,
                                    int slowQueries,
                                    int failedQueries,
                                    int cacheHits,
                                    long averageDurationMs,
                                    long timeoutPolicyMs,
                                    int datasetQuotaRows,
                                    List<DatasetQueryStats> datasets) {
        this(totalQueries, slowQueries, failedQueries, cacheHits, averageDurationMs, timeoutPolicyMs,
                datasetQuotaRows, datasets, slowAttributions(datasets));
    }

    public BiQueryGovernanceSummary {
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
        slowAttributions = slowAttributions == null ? List.of() : List.copyOf(slowAttributions);
    }

    public record DatasetQueryStats(
            String datasetKey,
            int totalQueries,
            int slowQueries,
            int failedQueries,
            int cacheHits,
            long averageDurationMs,
            long maxDurationMs,
            long timeoutPolicyMs,
            int quotaRows,
            int slowFailures,
            int slowCacheMisses,
            long maxOverPolicyMs,
            int maxRowCount
    ) {
        public DatasetQueryStats(String datasetKey,
                                 int totalQueries,
                                 int slowQueries,
                                 int failedQueries,
                                 int cacheHits,
                                 long averageDurationMs,
                                 long maxDurationMs,
                                 long timeoutPolicyMs,
                                 int quotaRows) {
            this(datasetKey, totalQueries, slowQueries, failedQueries, cacheHits, averageDurationMs, maxDurationMs,
                    timeoutPolicyMs, quotaRows, 0, 0, Math.max(0L, maxDurationMs - timeoutPolicyMs), 0);
        }
    }

    public record SlowQueryAttribution(
            String datasetKey,
            int slowQueries,
            long maxDurationMs,
            long timeoutPolicyMs,
            long maxOverPolicyMs) {
    }

    private static List<SlowQueryAttribution> slowAttributions(List<DatasetQueryStats> datasets) {
        if (datasets == null || datasets.isEmpty()) {
            return List.of();
        }
        return datasets.stream()
                .filter(dataset -> dataset != null && dataset.slowQueries() > 0)
                .map(dataset -> new SlowQueryAttribution(
                        dataset.datasetKey(),
                        dataset.slowQueries(),
                        dataset.maxDurationMs(),
                        dataset.timeoutPolicyMs(),
                        dataset.maxOverPolicyMs()))
                .toList();
    }
}
