package org.chovy.canvas.bi.api;

public record BiChartDashboardReferenceView(
        String dashboardKey,
        String title,
        String widgetKey,
        String widgetTitle,
        String status) {
}
