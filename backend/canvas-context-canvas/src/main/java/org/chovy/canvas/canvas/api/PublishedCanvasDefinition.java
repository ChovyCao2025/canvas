package org.chovy.canvas.canvas.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PublishedCanvasDefinition(
        Long tenantId,
        Long canvasId,
        Long versionId,
        Integer version,
        String graphJson,
        Instant publishedAt,
        Map<String, Object> executionOptions,
        List<PublishedCanvasNodeDefinition> nodes,
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

    private static Long requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
