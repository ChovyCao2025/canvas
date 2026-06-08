package org.chovy.canvas.domain.bi.query;

import java.util.List;

/**
 * BiQueryGovernancePolicyUpdateCommand 承载对应领域的业务规则、流程编排和结果转换。
 */
public record BiQueryGovernancePolicyUpdateCommand(
        Long defaultTimeoutMs,
        Integer defaultQuotaRows,
        List<DatasetPolicyCommand> datasets) {

    public BiQueryGovernancePolicyUpdateCommand {
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }

    /**
     * DatasetPolicyCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record DatasetPolicyCommand(
            String datasetKey,
            Long timeoutMs,
            Integer quotaRows) {
    }
}
