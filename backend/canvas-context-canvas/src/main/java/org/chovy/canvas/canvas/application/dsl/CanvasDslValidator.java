package org.chovy.canvas.canvas.application.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chovy.canvas.canvas.api.dsl.CanvasDslDocument;
import org.springframework.stereotype.Component;

@Component
public class CanvasDslValidator {

    public static final String SUPPORTED_API_VERSION = "canvas/v1";

    private static final Set<String> SUPPORTED_NODE_TYPES = Set.of(
            "webhook",
            "condition",
            "message",
            "coupon",
            "approval",
            "ai",
            "risk-check",
            "end");

    public CanvasDslValidationResult validate(CanvasDslDocument document) {
        List<CanvasDslValidationResult.Violation> violations = new ArrayList<>();
        if (!SUPPORTED_API_VERSION.equals(document.apiVersion())) {
            violations.add(new CanvasDslValidationResult.Violation(
                    "UNSUPPORTED_API_VERSION",
                    "Only " + SUPPORTED_API_VERSION + " is supported"));
        }
        if (!"Journey".equals(document.kind())) {
            violations.add(new CanvasDslValidationResult.Violation(
                    "UNSUPPORTED_KIND",
                    "Only Journey kind is supported"));
        }
        if (document.metadata().name().isBlank()) {
            violations.add(new CanvasDslValidationResult.Violation(
                    "MISSING_METADATA_NAME",
                    "metadata.name is required"));
        }

        Set<String> nodeIds = new LinkedHashSet<>();
        Set<String> duplicateNodeIds = new LinkedHashSet<>();
        for (CanvasDslDocument.Node node : document.spec().nodes()) {
            if (node.id().isBlank()) {
                violations.add(new CanvasDslValidationResult.Violation(
                        "MISSING_NODE_ID",
                        "nodes[].id is required"));
                continue;
            }
            if (!nodeIds.add(node.id())) {
                duplicateNodeIds.add(node.id());
            }
            if (!SUPPORTED_NODE_TYPES.contains(node.type())) {
                violations.add(new CanvasDslValidationResult.Violation(
                        "UNSUPPORTED_NODE_TYPE",
                        "Unsupported node type: " + node.type()));
            }
        }
        for (String duplicateNodeId : duplicateNodeIds) {
            violations.add(new CanvasDslValidationResult.Violation(
                    "DUPLICATE_NODE_ID",
                    "Duplicate node id: " + duplicateNodeId));
        }

        for (CanvasDslDocument.Edge edge : document.spec().edges()) {
            if (!nodeIds.contains(edge.from())) {
                violations.add(new CanvasDslValidationResult.Violation(
                        "UNKNOWN_EDGE_SOURCE",
                        "Unknown edge source: " + edge.from()));
            }
            if (!nodeIds.contains(edge.to())) {
                violations.add(new CanvasDslValidationResult.Violation(
                        "UNKNOWN_EDGE_TARGET",
                        "Unknown edge target: " + edge.to()));
            }
        }
        if (containsCycle(nodeIds, document.spec().edges())) {
            violations.add(new CanvasDslValidationResult.Violation(
                    "GRAPH_CONTAINS_CYCLE",
                    "DSL graph must be a DAG"));
        }

        if (violations.isEmpty()) {
            return CanvasDslValidationResult.passed();
        }
        return CanvasDslValidationResult.failed(violations);
    }

    private static boolean containsCycle(Set<String> nodeIds, List<CanvasDslDocument.Edge> edges) {
        Map<String, List<String>> adjacency = new HashMap<>();
        for (String nodeId : nodeIds) {
            adjacency.put(nodeId, new ArrayList<>());
        }
        for (CanvasDslDocument.Edge edge : edges) {
            if (nodeIds.contains(edge.from()) && nodeIds.contains(edge.to())) {
                adjacency.get(edge.from()).add(edge.to());
            }
        }
        Set<String> visiting = new LinkedHashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        for (String nodeId : nodeIds) {
            if (hasCycle(nodeId, adjacency, visiting, visited)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCycle(String nodeId,
                                    Map<String, List<String>> adjacency,
                                    Set<String> visiting,
                                    Set<String> visited) {
        if (visited.contains(nodeId)) {
            return false;
        }
        if (!visiting.add(nodeId)) {
            return true;
        }
        for (String next : adjacency.getOrDefault(nodeId, List.of())) {
            if (hasCycle(next, adjacency, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(nodeId);
        visited.add(nodeId);
        return false;
    }
}
