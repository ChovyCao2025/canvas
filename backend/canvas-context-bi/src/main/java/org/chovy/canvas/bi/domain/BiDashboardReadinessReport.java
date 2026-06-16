package org.chovy.canvas.bi.domain;

import java.util.List;
/**
 * BiDashboardReadinessReport 不可变数据载体。
 */
public record BiDashboardReadinessReport(
        /**
         * 状态值。
         */
        String status,
        /**
         * productionReady 字段值。
         */
        boolean productionReady,
        /**
         * publishedChartCount 对应的统计数量。
         */
        int publishedChartCount,
        /**
         * draftDatasetCount 对应的统计数量。
         */
        int draftDatasetCount,
        /**
         * blockers 对应的数据集合。
         */
        List<BiDashboardReadinessIssue> blockers,
        /**
         * 告警问题列表。
         */
        List<BiDashboardReadinessIssue> warnings
) {
    public BiDashboardReadinessReport {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
