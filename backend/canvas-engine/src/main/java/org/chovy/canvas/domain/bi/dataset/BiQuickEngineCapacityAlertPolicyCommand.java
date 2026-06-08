package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

/**
 * BiQuickEngineCapacityAlertPolicyCommand 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param enabled enabled 字段。
 * @param capacityLimitRows capacityLimitRows 字段。
 * @param warningThresholdPercent warningThresholdPercent 字段。
 * @param criticalThresholdPercent criticalThresholdPercent 字段。
 * @param notificationChannels notificationChannels 字段。
 * @param notificationReceivers notificationReceivers 字段。
 */
public record BiQuickEngineCapacityAlertPolicyCommand(
        Boolean enabled,
        Long capacityLimitRows,
        Integer warningThresholdPercent,
        Integer criticalThresholdPercent,
        List<String> notificationChannels,
        List<String> notificationReceivers) {
}
