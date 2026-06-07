package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record MarketingMonitorAnomalyEventView(
        Long id,
        Long tenantId,
        Long ruleId,
        String ruleKey,
        Long sourceId,
        String sourceKey,
        String metricKey,
        String bucketGrain,
        LocalDateTime bucketStart,
        LocalDateTime bucketEnd,
        String brandKey,
        String competitorKey,
        BigDecimal actualValue,
        BigDecimal baselineMedian,
        BigDecimal baselineMad,
        BigDecimal robustZScore,
        BigDecimal deltaValue,
        String direction,
        String severity,
        String status,
        Map<String, Object> evidence,
        String createdBy,
        String resolvedBy,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
