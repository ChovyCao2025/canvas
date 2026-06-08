package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.chart.BiChartResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardPreset;

import java.util.List;

/**
 * BiDashboardDraftPlan 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param dashboard dashboard 字段。
 * @param charts charts 字段。
 * @param explanation explanation 字段。
 */
public record BiDashboardDraftPlan(
        BiDashboardPreset dashboard,
        List<BiChartResource> charts,
        String explanation
) {
    public BiDashboardDraftPlan {
        charts = charts == null ? List.of() : List.copyOf(charts);
    }
}
