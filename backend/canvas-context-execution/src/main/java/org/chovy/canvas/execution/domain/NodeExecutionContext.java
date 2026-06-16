package org.chovy.canvas.execution.domain;

import java.util.Map;

/**
 * 定义 NodeExecutionContext 的执行上下文数据结构或业务契约。
 * @param executionId executionId 对应的数据字段
 * @param node node 对应的数据字段
 * @param userId userId 对应的数据字段
 * @param payload payload 对应的数据字段
 * @param contextData contextData 对应的数据字段
 */
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
