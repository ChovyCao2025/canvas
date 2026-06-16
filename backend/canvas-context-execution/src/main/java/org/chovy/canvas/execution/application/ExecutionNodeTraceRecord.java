package org.chovy.canvas.execution.application;

import java.time.Instant;
import java.util.Map;

public record ExecutionNodeTraceRecord(
        Long tenantId,
        String executionId,
        String nodeId,
        String nodeType,
        String status,
        String error,
        Map<String, Object> outputData,
        Instant occurredAt) {

    public ExecutionNodeTraceRecord {
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
        error = error == null ? "" : error;
        outputData = Map.copyOf(outputData == null ? Map.of() : outputData);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
