package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.util.Map;

public record MarketingMonitorAnomalyRuleCommand(
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
        Boolean enabled,
        Map<String, Object> metadata) {
}
