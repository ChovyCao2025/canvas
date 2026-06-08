package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

/**
 * BiQuickEngineCapacitySummaryView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param capacityLimitRows capacityLimitRows 字段。
 * @param usedRows usedRows 字段。
 * @param usagePercent usagePercent 字段。
 * @param alertLevel alertLevel 字段。
 * @param alertEnabled alertEnabled 字段。
 * @param alertPolicy alertPolicy 字段。
 * @param tenantPoolPolicy tenantPoolPolicy 字段。
 * @param concurrencyQueue concurrencyQueue 字段。
 * @param categories categories 字段。
 * @param details details 字段。
 * @param userRankings userRankings 字段。
 */
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
