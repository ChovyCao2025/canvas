package org.chovy.canvas.execution.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ExecutionContext {

    private final String executionId;
    private final Long tenantId;
    private final Long canvasId;
    private final int maxSizeBytes;
    private final Map<String, Map<String, Object>> nodeOutputs = new LinkedHashMap<>();

    public ExecutionContext(String executionId, Long tenantId, Long canvasId, int maxSizeBytes) {
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("executionId is required");
        }
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (canvasId == null || canvasId <= 0) {
            throw new IllegalArgumentException("canvasId is required");
        }
        if (maxSizeBytes < 1) {
            throw new IllegalArgumentException("maxSizeBytes must be positive");
        }
        this.executionId = executionId;
        this.tenantId = tenantId;
        this.canvasId = canvasId;
        this.maxSizeBytes = maxSizeBytes;
    }

    public String executionId() {
        return executionId;
    }

    public Long tenantId() {
        return tenantId;
    }

    public Long canvasId() {
        return canvasId;
    }

    public synchronized void putNodeOutput(String nodeId, Map<String, Object> output) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId is required");
        }
        Map<String, Map<String, Object>> candidate = new LinkedHashMap<>(nodeOutputs);
        candidate.put(nodeId, Map.copyOf(output == null ? Map.of() : output));
        int size = serializedSize(candidate);
        if (size > maxSizeBytes) {
            throw new IllegalStateException("context size limit exceeded: " + size + " > " + maxSizeBytes);
        }
        nodeOutputs.clear();
        nodeOutputs.putAll(candidate);
    }

    public synchronized Map<String, Object> nodeOutput(String nodeId) {
        return nodeOutputs.containsKey(nodeId) ? nodeOutputs.get(nodeId) : Map.of();
    }

    public synchronized Map<String, Map<String, Object>> nodeOutputs() {
        return Map.copyOf(nodeOutputs);
    }

    private int serializedSize(Map<String, Map<String, Object>> value) {
        return value.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }
}
