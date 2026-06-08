package org.chovy.canvas.domain.bi.chart;

import java.util.List;

/**
 * 汇总 BI 图表在删除图表或数据集前需要检查的引用位置。
 *
 * @param chartKey 被检查图表的稳定标识
 * @param chartName 删除提醒和审计信息中展示的图表名称
 * @param datasetKey 图表所属或依赖的数据集标识
 * @param dashboards 引用该图表的仪表盘组件
 * @param portals 暴露该图表的门户菜单项
 * @param subscriptions 包含该图表的定时订阅
 */
public record BiChartReferenceImpact(
        String chartKey,
        String chartName,
        String datasetKey,
        List<BiChartDashboardReference> dashboards,
        List<BiChartPortalReference> portals,
        List<BiChartSubscriptionReference> subscriptions
) {
    /**
     * 规范化可选引用集合，便于下游无空值检查地遍历且不能修改检查结果。
     */
    public BiChartReferenceImpact {
        dashboards = dashboards == null ? List.of() : List.copyOf(dashboards);
        portals = portals == null ? List.of() : List.copyOf(portals);
        subscriptions = subscriptions == null ? List.of() : List.copyOf(subscriptions);
    }
}
