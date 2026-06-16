package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiQueryGovernanceSummaryView 视图。
 */
public record BiQueryGovernanceSummaryView(
        /**
         * totalQueries 对应的数据集合。
         */
        int totalQueries,
        /**
         * slowQueries 对应的数据集合。
         */
        int slowQueries,
        /**
         * failedQueries 对应的数据集合。
         */
        int failedQueries,
        /**
         * cacheHits 对应的数据集合。
         */
        int cacheHits,
        /**
         * averageDurationMs 对应的数据集合。
         */
        long averageDurationMs,
        /**
         * timeoutPolicyMs 对应的数据集合。
         */
        long timeoutPolicyMs,
        /**
         * datasetQuotaRows 对应的数据集合。
         */
        int datasetQuotaRows,
        /**
         * datasets 对应的数据集合。
         */
        List<Map<String, Object>> datasets,
        List<Map<String, Object>> slowAttributions) {

    public BiQueryGovernanceSummaryView {
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
        slowAttributions = slowAttributions == null ? List.of() : List.copyOf(slowAttributions);
    }
}
