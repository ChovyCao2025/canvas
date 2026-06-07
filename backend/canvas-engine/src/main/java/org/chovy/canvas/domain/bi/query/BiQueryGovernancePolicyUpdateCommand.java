package org.chovy.canvas.domain.bi.query;

import java.util.List;

public record BiQueryGovernancePolicyUpdateCommand(
        Long defaultTimeoutMs,
        Integer defaultQuotaRows,
        List<DatasetPolicyCommand> datasets) {

    public BiQueryGovernancePolicyUpdateCommand {
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }

    public record DatasetPolicyCommand(
            String datasetKey,
            Long timeoutMs,
            Integer quotaRows) {
    }
}
