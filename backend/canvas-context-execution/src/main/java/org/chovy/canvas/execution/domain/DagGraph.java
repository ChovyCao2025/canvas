package org.chovy.canvas.execution.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 定义 DagGraph 的执行上下文数据结构或业务契约。
 */
public final class DagGraph {

    /**
     * 保存 Map<String 对应的状态或配置。
     */
    private final Map<String, DagNode> nodes;

    /**
     * 保存 Map<String 对应的状态或配置。
     */
    private final Map<String, List<String>> forward;

    /**
     * 保存 Map<String 对应的状态或配置。
     */
    private final Map<String, List<String>> reverse;

    /**
     * 保存 Map<String 对应的状态或配置。
     */
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

    /**
     * 执行 node 对应的业务处理。
     * @param nodeId nodeId 参数
     * @return 处理后的结果
     */
    public DagNode node(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * 执行 downstream 对应的业务处理。
     * @param nodeId nodeId 参数
     * @return 处理后的结果
     */
    public List<String> downstream(String nodeId) {
        return forward.getOrDefault(nodeId, List.of());
    }

    /**
     * 执行 upstream 对应的业务处理。
     * @param nodeId nodeId 参数
     * @return 处理后的结果
     */
    public List<String> upstream(String nodeId) {
        return reverse.getOrDefault(nodeId, List.of());
    }

    /**
     * 执行 entryNodes 对应的业务处理。
     * @return 处理后的结果
     */
    public List<String> entryNodes() {
        return inDegree.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * 执行 allNodeIds 对应的业务处理。
     * @return 处理后的结果
     */
    public Set<String> allNodeIds() {
        return nodes.keySet();
    }

    /**
     * 执行 nodes 对应的业务处理。
     * @return 处理后的结果
     */
    public Map<String, DagNode> nodes() {
        return nodes;
    }

    /**
     * 执行 copyLists 对应的业务处理。
     * @param source source 参数
     * @return 处理后的结果
     */
    private static Map<String, List<String>> copyLists(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Map.copyOf(copy);
    }
}
