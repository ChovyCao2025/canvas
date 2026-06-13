package org.chovy.canvas.bi.api;

import java.util.List;

public record BiDashboardPresetWidgetView(
        String widgetKey,
        String title,
        String chartType,
        List<String> dimensions,
        List<String> metrics,
        int gridX,
        int gridY,
        int gridW,
        int gridH,
        String stylePreset) {

    public BiDashboardPresetWidgetView {
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
