package org.chovy.canvas.canvas.api;

import java.util.Map;

/**
 * 承载PublishedCanvasNodeDefinition的数据快照。
 */
public record PublishedCanvasNodeDefinition(
        /**
         * 记录节点标识。
         */
        String nodeId,
        /**
         * 记录nodeType。
         */
        String nodeType,
        /**
         * 记录displayName。
         */
        String displayName,
        /**
         * 记录配置JSON 内容。
         */
        String configJson,
        /**
         * 记录position。
         */
        Map<String, Object> position,
        /**
         * 记录元数据。
         */
        Map<String, Object> metadata) {

    public PublishedCanvasNodeDefinition {
        nodeId = requireText(nodeId, "nodeId");
        nodeType = requireText(nodeType, "nodeType");
        displayName = displayName == null ? "" : displayName;
        configJson = configJson == null ? "{}" : configJson;
        position = Map.copyOf(position == null ? Map.of() : position);
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
