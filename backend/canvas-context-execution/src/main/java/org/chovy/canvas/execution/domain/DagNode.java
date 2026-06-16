package org.chovy.canvas.execution.domain;

import java.util.Map;

/**
 * 定义 DagNode 的执行上下文数据结构或业务契约。
 * @param nodeId nodeId 对应的数据字段
 * @param nodeType nodeType 对应的数据字段
 * @param displayName displayName 对应的数据字段
 * @param config config 对应的数据字段
 * @param metadata metadata 对应的数据字段
 */
public record DagNode(
        String nodeId,
        String nodeType,
        String displayName,
        Map<String, Object> config,
        Map<String, Object> metadata) {

    public DagNode {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId is required");
        }
        if (nodeType == null || nodeType.isBlank()) {
            throw new IllegalArgumentException("nodeType is required");
        }
        displayName = displayName == null ? "" : displayName;
        config = Map.copyOf(config == null ? Map.of() : config);
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }
}
