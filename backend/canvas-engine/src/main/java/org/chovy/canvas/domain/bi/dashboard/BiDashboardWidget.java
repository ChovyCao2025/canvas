package org.chovy.canvas.domain.bi.dashboard;

import java.util.List;

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
