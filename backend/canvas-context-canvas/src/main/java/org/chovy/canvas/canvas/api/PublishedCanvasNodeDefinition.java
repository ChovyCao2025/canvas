package org.chovy.canvas.canvas.api;

import java.util.Map;

public record PublishedCanvasNodeDefinition(
        String nodeId,
        String nodeType,
        String displayName,
        String configJson,
        Map<String, Object> position,
        Map<String, Object> metadata) {

    public PublishedCanvasNodeDefinition {
        nodeId = requireText(nodeId, "nodeId");
        nodeType = requireText(nodeType, "nodeType");
        displayName = displayName == null ? "" : displayName;
        configJson = configJson == null ? "{}" : configJson;
        position = Map.copyOf(position == null ? Map.of() : position);
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
