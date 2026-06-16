package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiDeliveryRunResult 结果。
 */
public record BiDeliveryRunResult(
        /**
         * jobType 字段值。
         */
        String jobType,
        /**
         * jobId 对应的标识。
         */
        Long jobId,
        /**
         * jobKey 对应的业务键。
         */
        String jobKey,
        /**
         * 状态值。
         */
        String status,
        List<BiDeliveryLogView> logs) {

    public BiDeliveryRunResult {
        logs = logs == null ? List.of() : List.copyOf(logs);
    }
}
