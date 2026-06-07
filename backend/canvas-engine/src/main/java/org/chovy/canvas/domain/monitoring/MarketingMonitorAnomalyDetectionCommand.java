package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;

public record MarketingMonitorAnomalyDetectionCommand(
        Long ruleId,
        LocalDateTime bucketStart,
        LocalDateTime bucketEnd) {
}
