package org.chovy.canvas.domain.marketing;

import java.util.List;

/**
 * GrowthActivityReadinessView 承载 domain.marketing 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param activityId activityId 字段。
 * @param activityKey activityKey 字段。
 * @param activityType activityType 字段。
 * @param generatedAt generatedAt 字段。
 * @param status status 字段。
 * @param productionReady productionReady 字段。
 * @param blockerCount blockerCount 字段。
 * @param warningCount warningCount 字段。
 * @param blockers blockers 字段。
 * @param warnings warnings 字段。
 * @param checks checks 字段。
 */
public record GrowthActivityReadinessView(
        Long tenantId,
        Long activityId,
        String activityKey,
        String activityType,
        String generatedAt,
        String status,
        boolean productionReady,
        int blockerCount,
        int warningCount,
        List<GrowthActivityReadinessCheckView> blockers,
        List<GrowthActivityReadinessCheckView> warnings,
        List<GrowthActivityReadinessCheckView> checks) {
}
