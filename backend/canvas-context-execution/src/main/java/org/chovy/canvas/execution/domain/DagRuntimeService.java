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

/**
 * 定义 DagRuntimeService 的执行上下文数据结构或业务契约。
 */
@Service
public class DagRuntimeService {

    /**
     * 保存 configParser 对应的状态或配置。
     */
    private final NodeConfigParser configParser;

    /**
     * 执行 DagRuntimeService 对应的业务处理。
     */
    public DagRuntimeService() {
        this(NodeConfigParser.empty());
    }

    /**
     * 执行 DagRuntimeService 对应的业务处理。
     * @param configParser configParser 参数
     */
    public DagRuntimeService(NodeConfigParser configParser) {
        this.configParser = Objects.requireNonNull(configParser, "configParser is required");
    }

    /**
     * 执行 validate 对应的业务处理。
     * @param definition definition 参数
     * @return 处理后的结果
     */
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

        // 使用入度拓扑遍历校验环路，避免运行期调度进入无法完成的图。
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
                // 每消费一条入边就降低目标节点入度，入度清零代表可进入拓扑队列。
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
