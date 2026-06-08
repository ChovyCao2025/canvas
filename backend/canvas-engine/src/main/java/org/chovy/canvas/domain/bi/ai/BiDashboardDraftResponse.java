package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.chart.BiChartResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardPreset;

import java.util.List;

/**
 * BiDashboardDraftResponse 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param status status 字段。
 * @param fallbackUsed fallbackUsed 字段。
 * @param dashboard dashboard 字段。
 * @param charts charts 字段。
 * @param explanation explanation 字段。
 */
public record BiDashboardDraftResponse(
        String status,
        boolean fallbackUsed,
        BiDashboardPreset dashboard,
        List<BiChartResource> charts,
        String explanation
) {
    public BiDashboardDraftResponse {
        charts = charts == null ? List.of() : List.copyOf(charts);
    }
}
