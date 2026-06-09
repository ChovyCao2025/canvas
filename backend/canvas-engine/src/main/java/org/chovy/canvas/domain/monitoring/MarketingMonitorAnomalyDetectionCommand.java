package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;

/**
 * MarketingMonitorAnomalyDetectionCommand 承载 domain.monitoring 场景中的不可变数据快照。
 * @param ruleId ruleId 字段。
 * @param bucketStart bucketStart 字段。
 * @param bucketEnd bucketEnd 字段。
 */
public record MarketingMonitorAnomalyDetectionCommand(
        Long ruleId,
        LocalDateTime bucketStart,
        LocalDateTime bucketEnd) {
}
