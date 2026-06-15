package org.chovy.canvas.bi.api;

import java.util.List;

public record BiDeliveryAuditSummary(
        int total,
        int delivered,
        int triggered,
        int skipped,
        int pending,
        int failed,
        int retryable,
        int retryExhausted,
        List<BiDeliveryLogView> logs) {

    public BiDeliveryAuditSummary {
        logs = logs == null ? List.of() : List.copyOf(logs);
    }
}
