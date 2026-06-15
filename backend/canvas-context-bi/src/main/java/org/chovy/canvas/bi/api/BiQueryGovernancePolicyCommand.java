package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiQueryGovernancePolicyCommand(
        Long defaultTimeoutMs,
        Integer defaultQuotaRows,
        List<Map<String, Object>> datasets) {

    public BiQueryGovernancePolicyCommand {
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }
}
