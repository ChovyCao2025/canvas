package org.chovy.canvas.domain.bi.query;

import java.util.List;

/**
 * BiQueryGovernanceSummary 承载对应领域的业务规则、流程编排和结果转换。
 */
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
    /**
     * 初始化 BiQueryGovernanceSummary 实例。
     *
     * @param totalQueries total queries 参数，用于 BiQueryGovernanceSummary 流程中的校验、计算或对象转换。
     * @param slowQueries slow queries 参数，用于 BiQueryGovernanceSummary 流程中的校验、计算或对象转换。
     * @param failedQueries failed queries 参数，用于 BiQueryGovernanceSummary 流程中的校验、计算或对象转换。
     * @param cacheHits 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param averageDurationMs average duration ms 参数，用于 BiQueryGovernanceSummary 流程中的校验、计算或对象转换。
     * @param timeoutPolicyMs 时间参数，用于计算窗口、过期或审计时间。
     * @param datasetQuotaRows dataset quota rows 参数，用于 BiQueryGovernanceSummary 流程中的校验、计算或对象转换。
     * @param datasets datasets 参数，用于 BiQueryGovernanceSummary 流程中的校验、计算或对象转换。
     */
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

    /**
     * DatasetQueryStats 承载对应领域的业务规则、流程编排和结果转换。
     */
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
        /**
         * 初始化 DatasetQueryStats 实例。
         *
         * @param datasetKey 业务键，用于在同一租户下定位资源。
         * @param totalQueries total queries 参数，用于 DatasetQueryStats 流程中的校验、计算或对象转换。
         * @param slowQueries slow queries 参数，用于 DatasetQueryStats 流程中的校验、计算或对象转换。
         * @param failedQueries failed queries 参数，用于 DatasetQueryStats 流程中的校验、计算或对象转换。
         * @param cacheHits 依赖组件，用于完成数据访问、计算或外部能力调用。
         * @param averageDurationMs average duration ms 参数，用于 DatasetQueryStats 流程中的校验、计算或对象转换。
         * @param maxDurationMs max duration ms 参数，用于 DatasetQueryStats 流程中的校验、计算或对象转换。
         * @param timeoutPolicyMs 时间参数，用于计算窗口、过期或审计时间。
         * @param quotaRows quota rows 参数，用于 DatasetQueryStats 流程中的校验、计算或对象转换。
         */
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

    /**
     * SlowQueryAttribution 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record SlowQueryAttribution(
            String datasetKey,
            int slowQueries,
            long maxDurationMs,
            long timeoutPolicyMs,
            long maxOverPolicyMs) {
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param datasets datasets 参数，用于 slowAttributions 流程中的校验、计算或对象转换。
     * @return 返回 slow attributions 汇总后的集合、分页或映射视图。
     */
    private static List<SlowQueryAttribution> slowAttributions(List<DatasetQueryStats> datasets) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (datasets == null || datasets.isEmpty()) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
