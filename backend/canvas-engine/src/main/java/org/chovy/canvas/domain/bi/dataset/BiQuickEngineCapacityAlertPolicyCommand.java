package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

public record BiQuickEngineCapacityAlertPolicyCommand(
        Boolean enabled,
        Long capacityLimitRows,
        Integer warningThresholdPercent,
        Integer criticalThresholdPercent,
        List<String> notificationChannels,
        List<String> notificationReceivers) {
}
