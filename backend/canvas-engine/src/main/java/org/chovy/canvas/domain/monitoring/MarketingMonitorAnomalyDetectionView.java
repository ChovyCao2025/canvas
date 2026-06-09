package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;

/**
 * MarketingMonitorAnomalyDetectionView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param ruleId ruleId 字段。
 * @param ruleKey ruleKey 字段。
 * @param metricKey metricKey 字段。
 * @param status status 字段。
 * @param baselineBucketCount baselineBucketCount 字段。
 * @param actualValue actualValue 字段。
 * @param baselineMedian baselineMedian 字段。
 * @param baselineMad baselineMad 字段。
 * @param robustZScore robustZScore 字段。
 * @param deltaValue deltaValue 字段。
 * @param event event 字段。
 */
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
