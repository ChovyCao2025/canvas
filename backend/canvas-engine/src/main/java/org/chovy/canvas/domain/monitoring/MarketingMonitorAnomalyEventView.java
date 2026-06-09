package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * MarketingMonitorAnomalyEventView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param ruleId ruleId 字段。
 * @param ruleKey ruleKey 字段。
 * @param sourceId sourceId 字段。
 * @param sourceKey sourceKey 字段。
 * @param metricKey metricKey 字段。
 * @param bucketGrain bucketGrain 字段。
 * @param bucketStart bucketStart 字段。
 * @param bucketEnd bucketEnd 字段。
 * @param brandKey brandKey 字段。
 * @param competitorKey competitorKey 字段。
 * @param actualValue actualValue 字段。
 * @param baselineMedian baselineMedian 字段。
 * @param baselineMad baselineMad 字段。
 * @param robustZScore robustZScore 字段。
 * @param deltaValue deltaValue 字段。
 * @param direction direction 字段。
 * @param severity severity 字段。
 * @param status status 字段。
 * @param evidence evidence 字段。
 * @param createdBy createdBy 字段。
 * @param resolvedBy resolvedBy 字段。
 * @param resolvedAt resolvedAt 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
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
