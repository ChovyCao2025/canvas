package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiDashboardReadModelView 视图。
 */
public record BiDashboardReadModelView(
        /**
         * dashboard 字段值。
         */
        BiDashboardView dashboard,
        /**
         * charts 对应的数据集合。
         */
        List<BiChartView> charts,
        /**
         * datasets 对应的数据集合。
         */
        List<BiDatasetView> datasets,
        /**
         * 就绪检查结果。
         */
        BiDashboardReadinessView readiness
) {
    public BiDashboardReadModelView {
        charts = charts == null ? List.of() : List.copyOf(charts);
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }
}
