package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.List;
/**
 * BiQuickEngineCapacityAlertPolicyView 视图。
 */
public record BiQuickEngineCapacityAlertPolicyView(
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
        /**
         * notificationReceivers 对应的数据集合。
         */
        List<String> notificationReceivers,
        /**
         * updatedBy 字段值。
         */
        String updatedBy,
        LocalDateTime updatedAt) {
}
