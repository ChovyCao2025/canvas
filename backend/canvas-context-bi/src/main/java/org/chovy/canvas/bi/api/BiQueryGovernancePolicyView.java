package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiQueryGovernancePolicyView(
        long defaultTimeoutMs,
        int defaultQuotaRows,
        List<Map<String, Object>> datasets) {

    public BiQueryGovernancePolicyView {
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }
}
