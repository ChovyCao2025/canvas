package org.chovy.canvas.domain.marketing;

import java.util.List;

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
