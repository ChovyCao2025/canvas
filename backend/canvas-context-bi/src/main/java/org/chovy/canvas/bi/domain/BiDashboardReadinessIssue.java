package org.chovy.canvas.bi.domain;
/**
 * BiDashboardReadinessIssue 不可变数据载体。
 */
public record BiDashboardReadinessIssue(
        /**
         * severity 字段值。
         */
        String severity,
        /**
         * code 字段值。
         */
        String code,
        /**
         * itemType 字段值。
         */
        String itemType,
        /**
         * itemKey 对应的业务键。
         */
        String itemKey,
        /**
         * 提示消息。
         */
        String message
) {
}
