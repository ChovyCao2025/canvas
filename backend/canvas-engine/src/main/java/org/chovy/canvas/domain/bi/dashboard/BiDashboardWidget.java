package org.chovy.canvas.domain.bi.dashboard;

import java.util.List;

/**
 * BiDashboardWidget 承载 domain.bi.dashboard 场景中的不可变数据快照。
 * @param widgetKey widgetKey 字段。
 * @param title title 字段。
 * @param chartType chartType 字段。
 * @param dimensions dimensions 字段。
 * @param metrics metrics 字段。
 * @param gridX gridX 字段。
 * @param gridY gridY 字段。
 * @param gridW gridW 字段。
 * @param gridH gridH 字段。
 * @param stylePreset stylePreset 字段。
 */
public record BiDashboardWidget(
        String widgetKey,
        String title,
        String chartType,
        List<String> dimensions,
        List<String> metrics,
        int gridX,
        int gridY,
        int gridW,
        int gridH,
        String stylePreset
) {
    public BiDashboardWidget {
        dimensions = List.copyOf(dimensions);
        metrics = List.copyOf(metrics);
    }
}
