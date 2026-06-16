package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiDeliveryRetryResult 结果。
 */
public record BiDeliveryRetryResult(
        /**
         * checked 字段值。
         */
        int checked,
        /**
         * retried 字段值。
         */
        int retried,
        /**
         * delivered 字段值。
         */
        int delivered,
        /**
         * pending 字段值。
         */
        int pending,
        /**
         * failed 字段值。
         */
        int failed,
        List<BiDeliveryLogView> logs) {

    public BiDeliveryRetryResult {
        logs = logs == null ? List.of() : List.copyOf(logs);
    }
}
