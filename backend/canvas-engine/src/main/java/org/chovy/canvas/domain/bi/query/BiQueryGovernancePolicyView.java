package org.chovy.canvas.domain.bi.query;

import java.util.List;

/**
 * BiQueryGovernancePolicyView 承载对应领域的业务规则、流程编排和结果转换。
 */
public record BiQueryGovernancePolicyView(
        long defaultTimeoutMs,
        int defaultQuotaRows,
        List<DatasetPolicyView> datasets) {

    public BiQueryGovernancePolicyView {
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }

    /**
     * DatasetPolicyView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record DatasetPolicyView(
            String datasetKey,
            long maxDurationMs,
            int quotaRows) {
    }
}
