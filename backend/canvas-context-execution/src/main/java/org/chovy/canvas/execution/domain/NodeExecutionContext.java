package org.chovy.canvas.execution.domain;

import java.util.Map;

public record NodeExecutionContext(
        String executionId,
        DagNode node,
        String userId,
        Map<String, Object> payload,
        Map<String, Object> contextData) {

    public NodeExecutionContext(String executionId, DagNode node, Map<String, Object> payload) {
        this(executionId, node, "", payload, Map.of());
    }

    public NodeExecutionContext {
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("executionId is required");
        }
        if (node == null) {
            throw new IllegalArgumentException("node is required");
        }
        userId = userId == null ? "" : userId;
        payload = Map.copyOf(payload == null ? Map.of() : payload);
        contextData = Map.copyOf(contextData == null ? Map.of() : contextData);
    }
}
