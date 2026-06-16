package org.chovy.canvas.execution.api.trace;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 定义 ExecutionTraceView 的执行上下文数据结构或业务契约。
 * @param tenantId tenantId 对应的数据字段
 * @param executionId executionId 对应的数据字段
 * @param canvasId canvasId 对应的数据字段
 * @param status status 对应的数据字段
 * @param startedAt startedAt 对应的数据字段
 * @param finishedAt finishedAt 对应的数据字段
 * @param nodeResults nodeResults 对应的数据字段
 * @param failureReason failureReason 对应的数据字段
 */
public record ExecutionTraceView(
        Long tenantId,
        String executionId,
        Long canvasId,
        String status,
        Instant startedAt,
        Instant finishedAt,
        List<NodeResultView> nodeResults,
        String failureReason) {

    public ExecutionTraceView {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("executionId is required");
        }
        if (canvasId == null || canvasId <= 0) {
            throw new IllegalArgumentException("canvasId is required");
        }
        status = status == null || status.isBlank() ? "UNKNOWN" : status;
        nodeResults = List.copyOf(nodeResults == null ? List.of() : nodeResults);
        failureReason = failureReason == null ? "" : failureReason;
    }

    public record NodeResultView(
            String nodeId,
            String nodeType,
            String status,
            String error,
            Map<String, Object> outputData) {

        public NodeResultView {
            if (nodeId == null || nodeId.isBlank()) {
                throw new IllegalArgumentException("nodeId is required");
            }
            nodeType = nodeType == null ? "" : nodeType;
            status = status == null || status.isBlank() ? "UNKNOWN" : status;
            error = error == null ? "" : error;
            outputData = Map.copyOf(outputData == null ? Map.of() : outputData);
        }
    }
}
