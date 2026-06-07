package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

public record BiQuickEngineCapacitySummaryView(
        Long tenantId,
        long capacityLimitRows,
        long usedRows,
        double usagePercent,
        String alertLevel,
        boolean alertEnabled,
        BiQuickEngineCapacityAlertPolicyView alertPolicy,
        BiQuickEngineTenantPoolPolicyView tenantPoolPolicy,
        BiQuickEngineConcurrencyQueueView concurrencyQueue,
        List<BiQuickEngineCapacityCategoryUsageView> categories,
        List<BiQuickEngineCapacityUsageDetailView> details,
        List<BiQuickEngineCapacityUserUsageView> userRankings) {
}
