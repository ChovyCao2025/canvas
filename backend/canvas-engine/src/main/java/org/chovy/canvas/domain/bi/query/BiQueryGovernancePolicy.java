package org.chovy.canvas.domain.bi.query;

import java.util.Map;

public record BiQueryGovernancePolicy(
        long defaultTimeoutMs,
        int defaultQuotaRows,
        Map<String, DatasetPolicy> datasets
) {
    public static final long DEFAULT_TIMEOUT_MS = 30_000L;
    public static final int DEFAULT_QUOTA_ROWS = 1_000_000;

    public BiQueryGovernancePolicy {
        defaultTimeoutMs = defaultTimeoutMs <= 0 ? DEFAULT_TIMEOUT_MS : defaultTimeoutMs;
        defaultQuotaRows = defaultQuotaRows <= 0 ? DEFAULT_QUOTA_ROWS : defaultQuotaRows;
        datasets = datasets == null ? Map.of() : Map.copyOf(datasets);
    }

    public static BiQueryGovernancePolicy defaults() {
        return new BiQueryGovernancePolicy(DEFAULT_TIMEOUT_MS, DEFAULT_QUOTA_ROWS, Map.of());
    }

    public DatasetPolicy datasetPolicy(String datasetKey) {
        DatasetPolicy configured = datasets.get(datasetKey);
        return configured == null
                ? new DatasetPolicy(defaultTimeoutMs, defaultQuotaRows)
                : configured.withDefaults(defaultTimeoutMs, defaultQuotaRows);
    }

    public record DatasetPolicy(
            long timeoutMs,
            int quotaRows
    ) {
        DatasetPolicy withDefaults(long defaultTimeoutMs, int defaultQuotaRows) {
            return new DatasetPolicy(
                    timeoutMs <= 0 ? defaultTimeoutMs : timeoutMs,
                    quotaRows <= 0 ? defaultQuotaRows : quotaRows);
        }
    }
}
