package org.chovy.canvas.domain.bi.dataset;

/**
 * BiDatasetAccelerationPolicyCommand 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param enabled enabled 字段。
 * @param accelerationMode accelerationMode 字段。
 * @param refreshMode refreshMode 字段。
 * @param refreshIntervalMinutes refreshIntervalMinutes 字段。
 * @param ttlSeconds ttlSeconds 字段。
 * @param maxRows maxRows 字段。
 * @param cronExpression cronExpression 字段。
 */
public record BiDatasetAccelerationPolicyCommand(
        Boolean enabled,
        String accelerationMode,
        String refreshMode,
        Long refreshIntervalMinutes,
        Long ttlSeconds,
        Long maxRows,
        String cronExpression) {
}
