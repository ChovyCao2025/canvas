package org.chovy.canvas.execution.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 定义 ExecutionContext 的执行上下文数据结构或业务契约。
 */
public final class ExecutionContext {

    /**
     * 保存 executionId 对应的状态或配置。
     */
    private final String executionId;

    /**
     * 保存 tenantId 对应的状态或配置。
     */
    private final Long tenantId;

    /**
     * 保存 canvasId 对应的状态或配置。
     */
    private final Long canvasId;

    /**
     * 保存 maxSizeBytes 对应的状态或配置。
     */
    private final int maxSizeBytes;
    private final Map<String, Map<String, Object>> nodeOutputs = new LinkedHashMap<>();

    /**
     * 执行 ExecutionContext 对应的业务处理。
     * @param executionId executionId 参数
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     * @param maxSizeBytes maxSizeBytes 参数
     */
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

    /**
     * 执行 executionId 对应的业务处理。
     */
    public String executionId() {
        return executionId;
    }

    /**
     * 执行 tenantId 对应的业务处理。
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 执行 canvasId 对应的业务处理。
     */
    public Long canvasId() {
        return canvasId;
    }

    /**
     * 执行 putNodeOutput 对应的业务处理。
     * @param nodeId nodeId 参数
     * @param output output 参数
     */
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

    /**
     * 执行 nodeOutput 对应的业务处理。
     * @param nodeId nodeId 参数
     * @return 处理后的结果
     */
    public synchronized Map<String, Object> nodeOutput(String nodeId) {
        return nodeOutputs.containsKey(nodeId) ? nodeOutputs.get(nodeId) : Map.of();
    }

    /**
     * 执行 nodeOutputs 对应的业务处理。
     * @return 处理后的结果
     */
    public synchronized Map<String, Map<String, Object>> nodeOutputs() {
        return Map.copyOf(nodeOutputs);
    }

    /**
     * 执行 serializedSize 对应的业务处理。
     * @param value value 参数
     */
    private int serializedSize(Map<String, Map<String, Object>> value) {
        return value.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }
}
