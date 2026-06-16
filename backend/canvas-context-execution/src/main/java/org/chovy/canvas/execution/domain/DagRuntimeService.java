package org.chovy.canvas.execution.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasEdgeDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasNodeDefinition;
import org.springframework.stereotype.Service;

@Service
public class DagRuntimeService {

    private final NodeConfigParser configParser;

    public DagRuntimeService() {
        this(NodeConfigParser.empty());
    }

    public DagRuntimeService(NodeConfigParser configParser) {
        this.configParser = Objects.requireNonNull(configParser, "configParser is required");
    }

    public DagGraph validate(PublishedCanvasDefinition definition) {
        Objects.requireNonNull(definition, "definition is required");
        Map<String, DagNode> nodes = new LinkedHashMap<>();
        Map<String, List<String>> forward = new LinkedHashMap<>();
        Map<String, List<String>> reverse = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();

        for (PublishedCanvasNodeDefinition node : definition.nodes()) {
            if (nodes.containsKey(node.nodeId())) {
                throw new IllegalArgumentException("duplicate nodeId: " + node.nodeId());
            }
            nodes.put(node.nodeId(), new DagNode(
                    node.nodeId(),
                    node.nodeType(),
                    node.displayName(),
                    configParser.parse(node.configJson(), node.nodeId()),
                    node.metadata()));
            forward.put(node.nodeId(), new ArrayList<>());
            reverse.put(node.nodeId(), new ArrayList<>());
            inDegree.put(node.nodeId(), 0);
        }
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("published definition must contain at least one node");
        }

        for (PublishedCanvasEdgeDefinition edge : definition.edges()) {
            if (!nodes.containsKey(edge.sourceNodeId())) {
                throw new IllegalArgumentException("edge has unknown source: " + edge.sourceNodeId());
            }
            if (!nodes.containsKey(edge.targetNodeId())) {
                throw new IllegalArgumentException("edge has unknown target: " + edge.targetNodeId());
            }
            forward.get(edge.sourceNodeId()).add(edge.targetNodeId());
            reverse.get(edge.targetNodeId()).add(edge.sourceNodeId());
            inDegree.merge(edge.targetNodeId(), 1, Integer::sum);
        }

        validateNoCycle(nodes, forward, inDegree);
        return new DagGraph(nodes, forward, reverse, inDegree);
    }

    private void validateNoCycle(
            Map<String, DagNode> nodes,
            Map<String, List<String>> forward,
            Map<String, Integer> inDegree) {
        Queue<String> queue = new ArrayDeque<>();
        Map<String, Integer> degree = new LinkedHashMap<>(inDegree);
        degree.forEach((nodeId, value) -> {
            if (value == 0) {
                queue.add(nodeId);
            }
        });

        int processed = 0;
        while (!queue.isEmpty()) {
            String current = queue.remove();
            processed++;
            for (String next : forward.getOrDefault(current, List.of())) {
                int nextDegree = degree.merge(next, -1, Integer::sum);
                if (nextDegree == 0) {
                    queue.add(next);
                }
            }
        }

        if (processed != nodes.size()) {
            throw new IllegalArgumentException("DAG contains cycle");
        }
    }
}
