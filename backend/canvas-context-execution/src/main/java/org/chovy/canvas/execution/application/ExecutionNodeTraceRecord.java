package org.chovy.canvas.execution.application;

import java.time.Instant;
import java.util.Map;

/**
 * 定义 ExecutionNodeTraceRecord 的执行上下文数据结构或业务契约。
 * @param tenantId tenantId 对应的数据字段
 * @param executionId executionId 对应的数据字段
 * @param nodeId nodeId 对应的数据字段
 * @param nodeType nodeType 对应的数据字段
 * @param status status 对应的数据字段
 * @param error error 对应的数据字段
 * @param outputData outputData 对应的数据字段
 * @param occurredAt occurredAt 对应的数据字段
 */
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
