package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiDashboardPresetWidgetView 视图。
 */
public record BiDashboardPresetWidgetView(
        /**
         * widgetKey 对应的业务键。
         */
        String widgetKey,
        /**
         * 展示标题。
         */
        String title,
        /**
         * chartType 字段值。
         */
        String chartType,
        /**
         * dimensions 对应的数据集合。
         */
        List<String> dimensions,
        /**
         * 指标列表。
         */
        List<String> metrics,
        /**
         * gridX 字段值。
         */
        int gridX,
        /**
         * gridY 字段值。
         */
        int gridY,
        /**
         * gridW 字段值。
         */
        int gridW,
        /**
         * gridH 字段值。
         */
        int gridH,
        String stylePreset) {

    public BiDashboardPresetWidgetView {
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
