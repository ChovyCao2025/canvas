package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * MarketingMonitorAnomalyRuleView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param ruleKey ruleKey 字段。
 * @param displayName displayName 字段。
 * @param sourceId sourceId 字段。
 * @param metricKey metricKey 字段。
 * @param bucketGrain bucketGrain 字段。
 * @param brandKey brandKey 字段。
 * @param competitorKey competitorKey 字段。
 * @param direction direction 字段。
 * @param baselineWindowBuckets baselineWindowBuckets 字段。
 * @param minBaselineBuckets minBaselineBuckets 字段。
 * @param thresholdMultiplier thresholdMultiplier 字段。
 * @param minDelta minDelta 字段。
 * @param enabled enabled 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
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
