package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiQueryGovernancePolicyView 视图。
 */
public record BiQueryGovernancePolicyView(
        /**
         * defaultTimeoutMs 对应的数据集合。
         */
        long defaultTimeoutMs,
        /**
         * defaultQuotaRows 对应的数据集合。
         */
        int defaultQuotaRows,
        List<Map<String, Object>> datasets) {

    public BiQueryGovernancePolicyView {
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }
}
