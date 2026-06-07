package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.chart.BiChartResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardPreset;

import java.util.List;

public record BiDashboardDraftPlan(
        BiDashboardPreset dashboard,
        List<BiChartResource> charts,
        String explanation
) {
    public BiDashboardDraftPlan {
        charts = charts == null ? List.of() : List.copyOf(charts);
    }
}
