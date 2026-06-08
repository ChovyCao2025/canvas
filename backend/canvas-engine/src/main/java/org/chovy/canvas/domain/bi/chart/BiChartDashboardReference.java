package org.chovy.canvas.domain.bi.chart;

/**
 * 描述当前嵌入 BI 图表的仪表盘组件。
 *
 * @param dashboardKey 仪表盘稳定标识
 * @param title 影响提示中展示的仪表盘标题
 * @param widgetKey 仪表盘内图表组件的稳定标识
 * @param widgetTitle 用于定位引用位置的组件标题
 * @param status 仪表盘引用的生命周期状态，如草稿或已发布
 */
public record BiChartDashboardReference(
        String dashboardKey,
        String title,
        String widgetKey,
        String widgetTitle,
        String status
) {
}
