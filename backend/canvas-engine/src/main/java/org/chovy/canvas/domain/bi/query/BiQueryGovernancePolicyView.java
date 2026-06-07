package org.chovy.canvas.domain.bi.query;

import java.util.List;

public record BiQueryGovernancePolicyView(
        long defaultTimeoutMs,
        int defaultQuotaRows,
        List<DatasetPolicyView> datasets) {

    public BiQueryGovernancePolicyView {
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }

    public record DatasetPolicyView(
            String datasetKey,
            long maxDurationMs,
            int quotaRows) {
    }
}
