package org.chovy.canvas.engine.dag;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class DagGraph {
    private final Map<String, DagParser.CanvasNode> nodeMap;
    private final Map<String, List<String>> forward;   // nodeId → [下游 nodeId]
    private final Map<String, List<String>> reverse;   // nodeId → [上游 nodeId]
    private final Map<String, Integer> inDegree;

    public DagParser.CanvasNode getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

    public List<String> downstream(String nodeId) {
        return forward.getOrDefault(nodeId, List.of());
    }

    public List<String> upstream(String nodeId) {
        return reverse.getOrDefault(nodeId, List.of());
    }

    /** 找入边为 0 的节点（触发器节点） */
    public List<String> entryNodes() {
        return inDegree.entrySet().stream()
                .filter(e -> e.getValue() == 0)
                .map(Map.Entry::getKey)
                .toList();
    }
}
