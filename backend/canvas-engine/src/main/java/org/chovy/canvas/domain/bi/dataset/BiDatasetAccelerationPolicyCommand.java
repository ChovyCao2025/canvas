package org.chovy.canvas.domain.bi.dataset;

public record BiDatasetAccelerationPolicyCommand(
        Boolean enabled,
        String accelerationMode,
        String refreshMode,
        Long refreshIntervalMinutes,
        Long ttlSeconds,
        Long maxRows,
        String cronExpression) {
}
