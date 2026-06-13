package org.chovy.canvas.bi.api;

import java.util.List;

public record BiDashboardReadModelView(
        BiDashboardView dashboard,
        List<BiChartView> charts,
        List<BiDatasetView> datasets,
        BiDashboardReadinessView readiness
) {
    public BiDashboardReadModelView {
        charts = charts == null ? List.of() : List.copyOf(charts);
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }
}
