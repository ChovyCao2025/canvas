package org.chovy.canvas.domain.bi.subscription;

import java.util.List;

public record BiDeliveryRunResult(
        String jobType,
        Long jobId,
        String jobKey,
        String status,
        List<BiDeliveryLogView> logs
) {
    public BiDeliveryRunResult {
        logs = logs == null ? List.of() : List.copyOf(logs);
    }
}
