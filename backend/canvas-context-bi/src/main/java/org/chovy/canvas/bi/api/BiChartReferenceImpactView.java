package org.chovy.canvas.bi.api;

import java.util.List;

public record BiChartReferenceImpactView(
        String chartKey,
        String chartName,
        String datasetKey,
        List<BiChartDashboardReferenceView> dashboards,
        List<BiChartPortalReferenceView> portals,
        List<BiChartSubscriptionReferenceView> subscriptions) {
}
