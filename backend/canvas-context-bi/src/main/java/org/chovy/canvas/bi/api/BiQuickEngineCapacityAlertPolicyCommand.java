package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiQuickEngineCapacityAlertPolicyCommand 命令。
 */
public record BiQuickEngineCapacityAlertPolicyCommand(
        /**
         * enabled 字段值。
         */
        Boolean enabled,
        /**
         * capacityLimitRows 对应的数据集合。
         */
        Long capacityLimitRows,
        /**
         * warningThresholdPercent 字段值。
         */
        Integer warningThresholdPercent,
        /**
         * criticalThresholdPercent 字段值。
         */
        Integer criticalThresholdPercent,
        /**
         * notificationChannels 对应的数据集合。
         */
        List<String> notificationChannels,
        List<String> notificationReceivers) {
}
