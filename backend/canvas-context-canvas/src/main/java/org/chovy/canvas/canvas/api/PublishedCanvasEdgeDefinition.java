package org.chovy.canvas.canvas.api;

import java.util.Map;

/**
 * 承载PublishedCanvasEdgeDefinition的数据快照。
 */
public record PublishedCanvasEdgeDefinition(
        /**
         * 记录边标识。
         */
        String edgeId,
        /**
         * 记录source node标识。
         */
        String sourceNodeId,
        /**
         * 记录target node标识。
         */
        String targetNodeId,
        /**
         * 记录conditionJSON 内容。
         */
        String conditionJson,
        /**
         * 记录元数据。
         */
        Map<String, Object> metadata) {

    public PublishedCanvasEdgeDefinition {
        sourceNodeId = requireText(sourceNodeId, "sourceNodeId");
        targetNodeId = requireText(targetNodeId, "targetNodeId");
        edgeId = edgeId == null || edgeId.isBlank() ? sourceNodeId + "->" + targetNodeId : edgeId;
        conditionJson = conditionJson == null ? "{}" : conditionJson;
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }

    /**
     * 校验文本不能为空。
     */
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
