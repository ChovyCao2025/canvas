package org.chovy.canvas.execution.adapter.messaging;

import java.time.Instant;
import java.util.Map;

public record TraceEvent(
        Long tenantId,
        String executionId,
        String nodeId,
        String nodeType,
        String status,
        Map<String, Object> outputData,
        Instant occurredAt) {

    public TraceEvent {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("executionId is required");
        }
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId is required");
        }
        nodeType = nodeType == null ? "" : nodeType;
        status = status == null || status.isBlank() ? "UNKNOWN" : status;
        outputData = Map.copyOf(outputData == null ? Map.of() : outputData);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
