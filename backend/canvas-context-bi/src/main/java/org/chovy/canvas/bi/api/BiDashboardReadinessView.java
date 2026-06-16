package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiDashboardReadinessView 视图。
 */
public record BiDashboardReadinessView(
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
        List<BiDashboardReadinessIssueView> blockers,
        /**
         * 告警问题列表。
         */
        List<BiDashboardReadinessIssueView> warnings
) {
    public BiDashboardReadinessView {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
