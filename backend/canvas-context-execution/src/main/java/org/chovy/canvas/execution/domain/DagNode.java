package org.chovy.canvas.execution.domain;

import java.util.Map;

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
