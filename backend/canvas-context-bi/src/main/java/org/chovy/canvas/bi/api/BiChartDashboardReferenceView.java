package org.chovy.canvas.bi.api;
/**
 * BiChartDashboardReferenceView 视图。
 */
public record BiChartDashboardReferenceView(
        /**
         * 仪表盘键。
         */
        String dashboardKey,
        /**
         * 展示标题。
         */
        String title,
        /**
         * widgetKey 对应的业务键。
         */
        String widgetKey,
        /**
         * widgetTitle 字段值。
         */
        String widgetTitle,
        String status) {
}
