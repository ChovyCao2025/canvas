package org.chovy.canvas.domain.bi.subscription;

import java.util.List;

/**
 * BiDeliveryAuditSummary 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param total total 字段。
 * @param delivered delivered 字段。
 * @param triggered triggered 字段。
 * @param skipped skipped 字段。
 * @param pending pending 字段。
 * @param failed failed 字段。
 * @param retryable retryable 字段。
 * @param retryExhausted retryExhausted 字段。
 * @param logs logs 字段。
 */
public record BiDeliveryAuditSummary(
        int total,
        int delivered,
        int triggered,
        int skipped,
        int pending,
        int failed,
        int retryable,
        int retryExhausted,
        List<BiDeliveryLogView> logs
) {
    public BiDeliveryAuditSummary {
        logs = logs == null ? List.of() : List.copyOf(logs);
    }
}
