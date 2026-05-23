package org.chovy.canvas.engine.dag;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 解析 graph_json → 内存中的 DAG 结构（邻接表 + 反向邻接表 + 入边数）
 * 同时执行 Kahn 算法环检测。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DagParser {

    private final ObjectMapper objectMapper;

    public DagGraph parse(String graphJson) {
        try {
            CanvasGraph graph = objectMapper.readValue(graphJson, CanvasGraph.class);
            return buildGraph(graph.getNodes());
        } catch (Exception e) {
            throw new IllegalArgumentException("画布 JSON 解析失败: " + e.getMessage(), e);
        }
    }

    private DagGraph buildGraph(List<CanvasNode> nodes) {
        Map<String, CanvasNode> nodeMap = new LinkedHashMap<>();
        Map<String, List<String>> forward = new HashMap<>();  // nodeId → [下游]
        Map<String, List<String>> reverse = new HashMap<>();  // nodeId → [上游]
        Map<String, Integer> inDegree = new HashMap<>();

        for (CanvasNode n : nodes) {
            nodeMap.put(n.getId(), n);
            forward.put(n.getId(), new ArrayList<>());
            reverse.put(n.getId(), new ArrayList<>());
            inDegree.put(n.getId(), 0);
        }

        // 从节点 config 中提取边
        for (CanvasNode n : nodes) {
            for (String target : extractTargets(n)) {
                if (!nodeMap.containsKey(target)) continue;
                forward.get(n.getId()).add(target);
                reverse.get(target).add(n.getId());
                inDegree.merge(target, 1, Integer::sum);
            }
        }

        // Kahn 算法环检测
        validateNoCycle(nodeMap, forward, inDegree);

        return new DagGraph(nodeMap, forward, reverse, inDegree);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractTargets(CanvasNode node) {
        List<String> targets = new ArrayList<>();
        // config 优先，bizConfig 兜底（触发器节点 nextNodeId 只存在 bizConfig）
        Map<String, Object> c = new HashMap<>();
        if (node.getBizConfig() != null) c.putAll(node.getBizConfig());
        if (node.getConfig()    != null) c.putAll(node.getConfig());    // config 覆盖 bizConfig

        addIfPresent(targets, c.get("nextNodeId"));
        addIfPresent(targets, c.get("successNodeId"));
        addIfPresent(targets, c.get("failNodeId"));
        addIfPresent(targets, c.get("elseNodeId"));
        addIfPresent(targets, c.get("approveNodeId"));
        addIfPresent(targets, c.get("rejectNodeId"));

        List<?> branches = (List<?>) c.get("branches");
        if (branches != null) branches.forEach(b ->
                addIfPresent(targets, ((Map<?, ?>) b).get("nextNodeId")));

        List<?> priorities = (List<?>) c.get("priorities");
        if (priorities != null) priorities.forEach(p ->
                addIfPresent(targets, ((Map<?, ?>) p).get("nextNodeId")));

        List<?> groups = (List<?>) c.get("groups");
        if (groups != null) groups.forEach(g ->
                addIfPresent(targets, ((Map<?, ?>) g).get("nextNodeId")));

        return targets;
    }

    private void addIfPresent(List<String> list, Object val) {
        if (val instanceof String s && !s.isBlank()) list.add(s);
    }

    private void validateNoCycle(Map<String, CanvasNode> nodeMap,
                                  Map<String, List<String>> forward,
                                  Map<String, Integer> inDegree) {
        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> degree = new HashMap<>(inDegree);
        degree.forEach((id, deg) -> { if (deg == 0) queue.add(id); });

        int processed = 0;
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            processed++;
            for (String next : forward.getOrDefault(cur, List.of())) {
                int d = degree.merge(next, -1, Integer::sum);
                if (d == 0) queue.add(next);
            }
        }

        if (processed < nodeMap.size()) {
            Set<String> cycleNodes = new HashSet<>(nodeMap.keySet());
            degree.forEach((id, d) -> { if (d == 0) cycleNodes.remove(id); });
            throw new IllegalArgumentException("画布存在循环连接，涉及节点: " + cycleNodes);
        }
    }

    // ── 内部 DTO ─────────────────────────────────────────────────

    @Data
    public static class CanvasGraph {
        private List<CanvasNode> nodes = new ArrayList<>();
    }

    @Data
    public static class CanvasNode {
        private String id;
        private String type;
        private String name;
        private Map<String, Object> config;
        private Map<String, Object> bizConfig;
        private String outletSchema;
        private Double x;
        private Double y;
    }
}
