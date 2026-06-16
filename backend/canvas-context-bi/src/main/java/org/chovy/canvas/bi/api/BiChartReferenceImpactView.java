package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiChartReferenceImpactView 视图。
 */
public record BiChartReferenceImpactView(
        /**
         * 图表键。
         */
        String chartKey,
        /**
         * chartName 字段值。
         */
        String chartName,
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * dashboards 对应的数据集合。
         */
        List<BiChartDashboardReferenceView> dashboards,
        /**
         * portals 对应的数据集合。
         */
        List<BiChartPortalReferenceView> portals,
        List<BiChartSubscriptionReferenceView> subscriptions) {
}
