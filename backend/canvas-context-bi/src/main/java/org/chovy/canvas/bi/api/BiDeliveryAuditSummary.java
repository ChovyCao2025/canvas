package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiDeliveryAuditSummary 汇总视图。
 */
public record BiDeliveryAuditSummary(
        /**
         * total 对应的统计数量。
         */
        int total,
        /**
         * delivered 字段值。
         */
        int delivered,
        /**
         * triggered 字段值。
         */
        int triggered,
        /**
         * skipped 字段值。
         */
        int skipped,
        /**
         * pending 字段值。
         */
        int pending,
        /**
         * failed 字段值。
         */
        int failed,
        /**
         * retryable 字段值。
         */
        int retryable,
        /**
         * retryExhausted 字段值。
         */
        int retryExhausted,
        List<BiDeliveryLogView> logs) {

    public BiDeliveryAuditSummary {
        logs = logs == null ? List.of() : List.copyOf(logs);
    }
}
