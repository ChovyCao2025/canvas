package org.chovy.canvas.domain.bi.subscription;

import java.util.List;

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
