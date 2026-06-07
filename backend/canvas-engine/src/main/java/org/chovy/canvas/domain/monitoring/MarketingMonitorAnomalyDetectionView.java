package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;

public record MarketingMonitorAnomalyDetectionView(
        Long tenantId,
        Long ruleId,
        String ruleKey,
        String metricKey,
        String status,
        int baselineBucketCount,
        BigDecimal actualValue,
        BigDecimal baselineMedian,
        BigDecimal baselineMad,
        BigDecimal robustZScore,
        BigDecimal deltaValue,
        MarketingMonitorAnomalyEventView event) {
}
