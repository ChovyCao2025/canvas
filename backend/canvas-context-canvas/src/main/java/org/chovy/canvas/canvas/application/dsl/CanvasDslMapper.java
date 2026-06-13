package org.chovy.canvas.canvas.application.dsl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chovy.canvas.canvas.api.dsl.CanvasDslDocument;
import org.springframework.stereotype.Component;

@Component
public class CanvasDslMapper implements CanvasDslMappingService {

    @Override
    public MappingResult toGraphJson(CanvasDslDocument document) {
        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("dslVersion", document.apiVersion());
        graph.put("kind", document.kind());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", document.metadata().name());
        metadata.put("title", document.metadata().title());
        graph.put("metadata", metadata);

        Map<String, Object> trigger = new LinkedHashMap<>();
        trigger.put("type", document.spec().trigger().type());
        trigger.put("event", document.spec().trigger().event());
        graph.put("trigger", trigger);

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (CanvasDslDocument.Node node : document.spec().nodes()) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("id", node.id());
            mapped.put("type", node.type());
            mapped.put("config", new LinkedHashMap<>(node.config()));
            nodes.add(mapped);
        }
        graph.put("nodes", nodes);

        List<Map<String, Object>> edges = new ArrayList<>();
        for (CanvasDslDocument.Edge edge : document.spec().edges()) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("from", edge.from());
            mapped.put("to", edge.to());
            edges.add(mapped);
        }
        graph.put("edges", edges);

        return new MappingResult(document.metadata().name(), DslJsonSupport.toJson(graph));
    }

    @Override
    public CanvasDslDocument fromGraphJson(String graphJson) {
        Map<String, Object> graph = DslJsonSupport.parseObject(graphJson);
        Map<String, Object> metadata = objectValue(graph.get("metadata"));
        Map<String, Object> trigger = objectValue(graph.get("trigger"));

        List<CanvasDslDocument.Node> nodes = new ArrayList<>();
        for (Object rawNode : listValue(graph.get("nodes"))) {
            Map<String, Object> node = objectValue(rawNode);
            Map<String, Object> config = objectValue(node.get("config"));
            if (config.isEmpty()) {
                config = objectValue(objectValue(node.get("data")).get("config"));
            }
            nodes.add(new CanvasDslDocument.Node(
                    stringValue(node.get("id")),
                    stringValue(node.get("type")),
                    config));
        }

        List<CanvasDslDocument.Edge> edges = new ArrayList<>();
        for (Object rawEdge : listValue(graph.get("edges"))) {
            Map<String, Object> edge = objectValue(rawEdge);
            edges.add(new CanvasDslDocument.Edge(
                    firstText(edge, "from", "source", "sourceNodeId"),
                    firstText(edge, "to", "target", "targetNodeId")));
        }

        return new CanvasDslDocument(
                firstTextOrDefault(graph, "canvas/v1", "dslVersion", "apiVersion"),
                firstTextOrDefault(graph, "Journey", "kind"),
                new CanvasDslDocument.Metadata(
                        stringValue(metadata.get("name")),
                        firstText(metadata, "title", "displayName")),
                new CanvasDslDocument.Spec(
                        new CanvasDslDocument.Trigger(
                                stringValue(trigger.get("type")),
                                stringValue(trigger.get("event"))),
                        nodes,
                        edges));
    }

    @Override
    public List<CanvasDslValidationResult.Violation> inspectUnsupportedExportSemantics(String graphJson) {
        Map<String, Object> graph = DslJsonSupport.parseObject(graphJson);
        List<CanvasDslValidationResult.Violation> violations = new ArrayList<>();
        for (Object rawEdge : listValue(graph.get("edges"))) {
            Map<String, Object> edge = objectValue(rawEdge);
            if (hasText(edge.get("condition")) || hasMeaningfulValue(edge.get("conditionJson"))) {
                violations.add(new CanvasDslValidationResult.Violation(
                        "UNSUPPORTED_GRAPH_EDGE_SEMANTICS",
                        "Graph edge semantics are not representable in Canvas DSL v1"));
            }
        }
        return violations;
    }

    @Override
    public DiffResult diff(CanvasDslDocument source, CanvasDslDocument target) {
        List<DiffChange> changes = new ArrayList<>();
        if (!source.spec().trigger().equals(target.spec().trigger())) {
            changes.add(new DiffChange(
                    "TRIGGER_CHANGED",
                    "trigger",
                    source.spec().trigger().type() + ":" + source.spec().trigger().event(),
                    target.spec().trigger().type() + ":" + target.spec().trigger().event()));
        }

        Map<String, CanvasDslDocument.Node> sourceNodes = nodesById(source);
        Map<String, CanvasDslDocument.Node> targetNodes = nodesById(target);
        for (Map.Entry<String, CanvasDslDocument.Node> entry : sourceNodes.entrySet()) {
            CanvasDslDocument.Node targetNode = targetNodes.get(entry.getKey());
            if (targetNode == null) {
                changes.add(new DiffChange("NODE_REMOVED", "nodes." + entry.getKey(), entry.getValue().type(), null));
                continue;
            }
            if (!entry.getValue().type().equals(targetNode.type())) {
                changes.add(new DiffChange("NODE_TYPE_CHANGED", "nodes." + entry.getKey(), entry.getValue().type(), targetNode.type()));
            }
            if (!entry.getValue().config().equals(targetNode.config())) {
                changes.add(new DiffChange("NODE_CONFIG_CHANGED", "nodes." + entry.getKey() + ".config",
                        DslJsonSupport.toJson(entry.getValue().config()),
                        DslJsonSupport.toJson(targetNode.config())));
            }
        }
        for (Map.Entry<String, CanvasDslDocument.Node> entry : targetNodes.entrySet()) {
            if (!sourceNodes.containsKey(entry.getKey())) {
                changes.add(new DiffChange("NODE_ADDED", "nodes." + entry.getKey(), null, entry.getValue().type()));
            }
        }

        Set<String> sourceEdges = edgeKeys(source);
        Set<String> targetEdges = edgeKeys(target);
        for (String edge : sourceEdges) {
            if (!targetEdges.contains(edge)) {
                changes.add(new DiffChange("EDGE_REMOVED", "edges." + edge, edge, null));
            }
        }
        for (String edge : targetEdges) {
            if (!sourceEdges.contains(edge)) {
                changes.add(new DiffChange("EDGE_ADDED", "edges." + edge, null, edge));
            }
        }
        return new DiffResult(!changes.isEmpty(), changes);
    }

    private static Map<String, CanvasDslDocument.Node> nodesById(CanvasDslDocument document) {
        Map<String, CanvasDslDocument.Node> nodes = new LinkedHashMap<>();
        for (CanvasDslDocument.Node node : document.spec().nodes()) {
            nodes.put(node.id(), node);
        }
        return nodes;
    }

    private static Set<String> edgeKeys(CanvasDslDocument document) {
        Set<String> edges = new java.util.LinkedHashSet<>();
        for (CanvasDslDocument.Edge edge : document.spec().edges()) {
            edges.add(edge.from() + "->" + edge.to());
        }
        return edges;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectValue(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("Expected JSON object");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> listValue(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        throw new IllegalArgumentException("Expected JSON array");
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean hasText(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private static boolean hasMeaningfulValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return !text.isBlank();
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        return true;
    }

    private static String firstText(Map<String, Object> values, String... keys) {
        return firstTextOrDefault(values, "", keys);
    }

    private static String firstTextOrDefault(Map<String, Object> values, String defaultValue, String... keys) {
        for (String key : keys) {
            String value = stringValue(values.get(key));
            if (!value.isBlank()) {
                return value;
            }
        }
        return defaultValue;
    }

}
