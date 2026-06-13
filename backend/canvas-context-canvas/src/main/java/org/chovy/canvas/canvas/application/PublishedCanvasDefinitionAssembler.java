package org.chovy.canvas.canvas.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasEdgeDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasNodeDefinition;
import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasVersion;

final class PublishedCanvasDefinitionAssembler {

    private PublishedCanvasDefinitionAssembler() {
    }

    static PublishedCanvasDefinition assemble(Canvas canvas, CanvasVersion version, Instant publishedAt) {
        Map<String, Object> options = new LinkedHashMap<>(canvas.runtimeOptions().toExecutionOptions());
        options.put("source", "canvas-context-canvas");
        Map<String, Object> graph = JsonSupport.parseObject(version.graphJson());
        return new PublishedCanvasDefinition(
                canvas.tenantId(),
                canvas.id(),
                version.id(),
                version.version(),
                version.graphJson(),
                publishedAt,
                options,
                nodes(graph.get("nodes")),
                edges(graph.get("edges")));
    }

    private static List<PublishedCanvasNodeDefinition> nodes(Object rawNodes) {
        List<PublishedCanvasNodeDefinition> result = new ArrayList<>();
        for (Map<String, Object> node : mapList(rawNodes)) {
            String nodeId = text(first(node, "id", "nodeId"));
            String nodeType = text(first(node, "type", "nodeType"));
            String displayName = text(first(node, "displayName", "name"));
            Object config = first(node, "config", "configJson");
            Object position = node.get("position");
            Object metadata = node.get("metadata");
            result.add(new PublishedCanvasNodeDefinition(
                    nodeId,
                    nodeType,
                    displayName,
                    jsonObject(config),
                    objectMap(position),
                    objectMap(metadata)));
        }
        return result;
    }

    private static List<PublishedCanvasEdgeDefinition> edges(Object rawEdges) {
        List<PublishedCanvasEdgeDefinition> result = new ArrayList<>();
        for (Map<String, Object> edge : mapList(rawEdges)) {
            String source = text(first(edge, "sourceNodeId", "source", "from"));
            String target = text(first(edge, "targetNodeId", "target", "to"));
            Object condition = first(edge, "conditionJson", "condition");
            result.add(new PublishedCanvasEdgeDefinition(
                    text(first(edge, "edgeId", "id")),
                    source,
                    target,
                    jsonObject(condition),
                    objectMap(edge.get("metadata"))));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String jsonObject(Object value) {
        if (value == null) {
            return "{}";
        }
        if (value instanceof String text) {
            return text.isBlank() ? "{}" : text;
        }
        return JsonSupport.toJson(value);
    }

    private static Object first(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
