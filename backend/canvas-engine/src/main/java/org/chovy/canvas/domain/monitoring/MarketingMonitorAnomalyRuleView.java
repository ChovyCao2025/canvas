package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record MarketingMonitorAnomalyRuleView(
        Long id,
        Long tenantId,
        String ruleKey,
        String displayName,
        Long sourceId,
        String metricKey,
        String bucketGrain,
        String brandKey,
        String competitorKey,
        String direction,
        Integer baselineWindowBuckets,
        Integer minBaselineBuckets,
        BigDecimal thresholdMultiplier,
        BigDecimal minDelta,
        boolean enabled,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
