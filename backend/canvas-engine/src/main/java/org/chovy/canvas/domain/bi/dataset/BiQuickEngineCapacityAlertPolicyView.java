package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BiQuickEngineCapacityAlertPolicyView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param enabled enabled 字段。
 * @param capacityLimitRows capacityLimitRows 字段。
 * @param warningThresholdPercent warningThresholdPercent 字段。
 * @param criticalThresholdPercent criticalThresholdPercent 字段。
 * @param notificationChannels notificationChannels 字段。
 * @param notificationReceivers notificationReceivers 字段。
 * @param updatedBy updatedBy 字段。
 * @param updatedAt updatedAt 字段。
 */
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
