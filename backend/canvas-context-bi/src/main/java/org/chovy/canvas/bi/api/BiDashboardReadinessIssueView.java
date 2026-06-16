package org.chovy.canvas.bi.api;
/**
 * BiDashboardReadinessIssueView 视图。
 */
public record BiDashboardReadinessIssueView(
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
