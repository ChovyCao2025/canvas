package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.List;

public record BiQuickEngineCapacityAlertPolicyView(
        Boolean enabled,
        Long capacityLimitRows,
        Integer warningThresholdPercent,
        Integer criticalThresholdPercent,
        List<String> notificationChannels,
        List<String> notificationReceivers,
        String updatedBy,
        LocalDateTime updatedAt) {
}
