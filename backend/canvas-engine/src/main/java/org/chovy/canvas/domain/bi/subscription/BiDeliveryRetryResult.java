package org.chovy.canvas.domain.bi.subscription;

import java.util.List;

/**
 * BiDeliveryRetryResult 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param checked checked 字段。
 * @param retried retried 字段。
 * @param delivered delivered 字段。
 * @param pending pending 字段。
 * @param failed failed 字段。
 * @param logs logs 字段。
 */
public record BiDeliveryRetryResult(
        int checked,
        int retried,
        int delivered,
        int pending,
        int failed,
        List<BiDeliveryLogView> logs
) {
    public BiDeliveryRetryResult {
        logs = logs == null ? List.of() : List.copyOf(logs);
    }
}
