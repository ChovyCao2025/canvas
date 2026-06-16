package org.chovy.canvas.execution.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DagGraph {

    private final Map<String, DagNode> nodes;
    private final Map<String, List<String>> forward;
    private final Map<String, List<String>> reverse;
    private final Map<String, Integer> inDegree;

    DagGraph(
            Map<String, DagNode> nodes,
            Map<String, List<String>> forward,
            Map<String, List<String>> reverse,
            Map<String, Integer> inDegree) {
        this.nodes = Map.copyOf(nodes);
        this.forward = copyLists(forward);
        this.reverse = copyLists(reverse);
        this.inDegree = Map.copyOf(inDegree);
    }

    public DagNode node(String nodeId) {
        return nodes.get(nodeId);
    }

    public List<String> downstream(String nodeId) {
        return forward.getOrDefault(nodeId, List.of());
    }

    public List<String> upstream(String nodeId) {
        return reverse.getOrDefault(nodeId, List.of());
    }

    public List<String> entryNodes() {
        return inDegree.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .toList();
    }

    public Set<String> allNodeIds() {
        return nodes.keySet();
    }

    public Map<String, DagNode> nodes() {
        return nodes;
    }

    private static Map<String, List<String>> copyLists(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Map.copyOf(copy);
    }
}
