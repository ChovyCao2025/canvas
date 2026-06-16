package org.chovy.canvas.canvas.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 承载PublishedCanvasDefinition的数据快照。
 */
public record PublishedCanvasDefinition(
        /**
         * 记录租户标识。
         */
        Long tenantId,
        /**
         * 记录画布标识。
         */
        Long canvasId,
        /**
         * 记录版本标识。
         */
        Long versionId,
        /**
         * 记录version。
         */
        Integer version,
        /**
         * 记录graphJSON 内容。
         */
        String graphJson,
        /**
         * 记录published时间。
         */
        Instant publishedAt,
        /**
         * 记录executionOptions。
         */
        Map<String, Object> executionOptions,
        /**
         * 记录nodes。
         */
        List<PublishedCanvasNodeDefinition> nodes,
        /**
         * 记录edges。
         */
        List<PublishedCanvasEdgeDefinition> edges) {

    public PublishedCanvasDefinition {
        tenantId = requirePositive(tenantId, "tenantId");
        canvasId = requirePositive(canvasId, "canvasId");
        versionId = requirePositive(versionId, "versionId");
        if (version == null || version <= 0) {
            throw new IllegalArgumentException("version is required");
        }
        if (graphJson == null || graphJson.isBlank()) {
            throw new IllegalArgumentException("graphJson is required");
        }
        if (publishedAt == null) {
            throw new IllegalArgumentException("publishedAt is required");
        }
        executionOptions = Map.copyOf(executionOptions == null ? Map.of() : executionOptions);
        nodes = List.copyOf(nodes == null ? List.of() : nodes);
        edges = List.copyOf(edges == null ? List.of() : edges);
    }

    /**
     * 校验数值必须为正数。
     */
    private static Long requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
