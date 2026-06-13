package org.chovy.canvas.canvas.api;

import java.util.Map;

public record PublishedCanvasEdgeDefinition(
        String edgeId,
        String sourceNodeId,
        String targetNodeId,
        String conditionJson,
        Map<String, Object> metadata) {

    public PublishedCanvasEdgeDefinition {
        sourceNodeId = requireText(sourceNodeId, "sourceNodeId");
        targetNodeId = requireText(targetNodeId, "targetNodeId");
        edgeId = edgeId == null || edgeId.isBlank() ? sourceNodeId + "->" + targetNodeId : edgeId;
        conditionJson = conditionJson == null ? "{}" : conditionJson;
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
