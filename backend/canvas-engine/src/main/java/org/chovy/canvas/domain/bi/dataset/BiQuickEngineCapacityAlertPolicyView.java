package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;
import java.util.List;

public record BiQuickEngineCapacityAlertPolicyView(
        boolean enabled,
        long capacityLimitRows,
        int warningThresholdPercent,
        int criticalThresholdPercent,
        List<String> notificationChannels,
        List<String> notificationReceivers,
        String updatedBy,
        LocalDateTime updatedAt) {
}
