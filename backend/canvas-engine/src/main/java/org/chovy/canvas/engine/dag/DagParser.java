package org.chovy.canvas.engine.dag;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
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

    /** JSON 解析器，用于反序列化前端保存的 graph_json。 */
    private final ObjectMapper objectMapper;

    /**
     * 入口：将前端保存的 graph_json 反序列化并构造成运行期图结构。
     * 失败时抛 IllegalArgumentException，交由上层接口统一转换错误码/文案。
     */
    public DagGraph parse(String graphJson) {
        try {
            CanvasGraph graph = objectMapper.readValue(graphJson, CanvasGraph.class);
            return buildGraph(graph.getNodes());
        } catch (Exception e) {
            throw new IllegalArgumentException("画布 JSON 解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建三份核心索引：
     * 1) nodeMap（节点定义）
     * 2) forward（下游邻接表）
     * 3) reverse（上游邻接表）
     * 并在末尾做一次环检测，保证运行期不会遇到显式环路。
     */
    private DagGraph buildGraph(List<CanvasNode> nodes) {
        Map<String, CanvasNode> nodeMap = new LinkedHashMap<>();
        Map<String, List<String>> forward = new HashMap<>();  // nodeId → [下游]
        Map<String, List<String>> reverse = new HashMap<>();  // nodeId → [上游]
        Map<String, Integer> inDegree = new HashMap<>();

        for (CanvasNode n : nodes) {
            nodeMap.put(n.getId(), n);
            // 先为每个节点初始化三份索引，后续即使没有边也能直接查询到空列表/0 入度。
            forward.put(n.getId(), new ArrayList<>());
            reverse.put(n.getId(), new ArrayList<>());
            inDegree.put(n.getId(), 0);
        }

        // 从节点配置提取边，忽略“指向不存在节点”的脏引用
        for (CanvasNode n : nodes) {
            for (String target : extractTargets(n)) {
                if (!nodeMap.containsKey(target)) continue;
                // 同时维护正向、反向和入度索引，运行期路由和入口节点判断都依赖这些结构。
                forward.get(n.getId()).add(target);
                reverse.get(target).add(n.getId());
                inDegree.merge(target, 1, Integer::sum);
            }
        }

        // Kahn 算法环检测
        validateNoCycle(nodeMap, forward, inDegree);
        validateMultiInputConvergence(nodeMap, reverse);

        return new DagGraph(nodeMap, forward, reverse, inDegree);
    }

    /**
     * 执行 extract Targets 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param node node 节点相关对象、标识或配置
     * @return 查询、转换或计算得到的结果集合
     */
    @SuppressWarnings("unchecked")
    private List<String> extractTargets(CanvasNode node) {
        List<String> targets = new ArrayList<>();
        // config 优先，bizConfig 兜底（触发器节点 nextNodeId 只存在 bizConfig）
        Map<String, Object> c = new HashMap<>();
        if (node.getBizConfig() != null) c.putAll(node.getBizConfig());
        if (node.getConfig()    != null) c.putAll(node.getConfig());    // config 覆盖 bizConfig

        addIfPresent(targets, c.get(MapFieldKeys.NEXT_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.SUCCESS_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.FAIL_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.ELSE_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.APPROVE_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.REJECT_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.TIMEOUT_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.SUPPRESSED_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.ALLOWED_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.QUIET_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.AVAILABLE_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.UNAVAILABLE_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.PASS_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.CAPPED_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.SKIPPED_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.MAX_EXCEEDED_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.GOAL_MET_NODE_ID));
        addIfPresent(targets, c.get(MapFieldKeys.GOAL_NOT_MET_NODE_ID));

        List<?> branches = (List<?>) c.get(MapFieldKeys.BRANCHES);
        if (branches != null) branches.forEach(b ->
                // 分支类节点的出口保存在数组项里，需要逐项展开为图边。
                addIfPresent(targets, ((Map<?, ?>) b).get(MapFieldKeys.NEXT_NODE_ID)));

        List<?> priorities = (List<?>) c.get(MapFieldKeys.PRIORITIES);
        if (priorities != null) priorities.forEach(p ->
                addIfPresent(targets, ((Map<?, ?>) p).get(MapFieldKeys.NEXT_NODE_ID)));

        List<?> groups = (List<?>) c.get(MapFieldKeys.GROUPS);
        if (groups != null) groups.forEach(g ->
                addIfPresent(targets, ((Map<?, ?>) g).get(MapFieldKeys.NEXT_NODE_ID)));

        List<?> paths = (List<?>) c.get(MapFieldKeys.PATHS);
        if (paths != null) paths.forEach(p ->
                addIfPresent(targets, ((Map<?, ?>) p).get(MapFieldKeys.NEXT_NODE_ID)));

        List<?> variants = (List<?>) c.get(MapFieldKeys.VARIANTS);
        if (variants != null) variants.forEach(v ->
                // 实验组/变体的 nextNodeId 也参与 DAG 校验，避免隐藏出口绕过环检测。
                addIfPresent(targets, ((Map<?, ?>) v).get(MapFieldKeys.NEXT_NODE_ID)));

        List<?> bands = (List<?>) c.get(MapFieldKeys.BANDS);
        if (bands != null) bands.forEach(b ->
                addIfPresent(targets, ((Map<?, ?>) b).get(MapFieldKeys.NEXT_NODE_ID)));

        // 保持原顺序返回，便于调试时与原始 JSON 对齐
        return targets;
    }

    /**
     * 创建或新增 add If Present 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param list list 待处理的数据集合
     * @param val val 方法执行所需的业务参数
     */
    private void addIfPresent(List<String> list, Object val) {
        if (val instanceof String s && !s.isBlank()) list.add(s);
    }

    /**
     * 校验 validate No Cycle 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param nodeMap nodeMap 节点相关对象、标识或配置
     * @param forward forward 方法执行所需的业务参数
     * @param inDegree inDegree 方法执行所需的业务参数
     */
    private void validateNoCycle(Map<String, CanvasNode> nodeMap,
                                  Map<String, List<String>> forward,
                                  Map<String, Integer> inDegree) {
        // Kahn：拷贝一份入度，避免污染原始索引（entryNodes 后续还要用）
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
            // 剩余入度 > 0 的节点即为环上节点（或受环影响节点）
            Set<String> cycleNodes = new HashSet<>(nodeMap.keySet());
            degree.forEach((id, d) -> { if (d == 0) cycleNodes.remove(id); });
            throw new IllegalArgumentException("画布存在循环连接，涉及节点: " + cycleNodes);
        }
    }

    private void validateMultiInputConvergence(Map<String, CanvasNode> nodeMap,
                                               Map<String, List<String>> reverse) {
        for (Map.Entry<String, List<String>> entry : reverse.entrySet()) {
            List<String> upstream = entry.getValue();
            if (upstream == null || upstream.size() <= 1) {
                continue;
            }
            CanvasNode node = nodeMap.get(entry.getKey());
            String type = node != null ? node.getType() : null;
            if (!isConvergenceNode(type)) {
                throw new IllegalArgumentException(
                        "多分支收敛必须使用 HUB/LOGIC_RELATION/AGGREGATE/THRESHOLD 节点: nodeId="
                                + entry.getKey() + " type=" + type + " upstream=" + upstream);
            }
        }
    }

    private boolean isConvergenceNode(String type) {
        return NodeType.HUB.equals(type)
                || NodeType.LOGIC_RELATION.equals(type)
                || NodeType.AGGREGATE.equals(type)
                || NodeType.THRESHOLD.equals(type);
    }

    // ── 内部 DTO ─────────────────────────────────────────────────

    /** graph_json 根对象。 */
    @Data
    public static class CanvasGraph {

        /** 节点列表。 */
        private List<CanvasNode> nodes = new ArrayList<>();
    }

    /** 节点定义，与前端 graph_json 节点结构对齐。 */
    @Data
    public static class CanvasNode {

        /** 节点 ID（前端生成的唯一标识）。 */
        private String id;

        /** 节点类型（对应 NodeType 常量）。 */
        private String type;

        /** 节点展示名称。 */
        private String name;

        /** 节点配置（运行时主配置）。 */
        private Map<String, Object> config;

        /** 节点业务配置（历史字段，解析时与 config 合并）。 */
        private Map<String, Object> bizConfig;

        /** 动态出口 schema（前端用于渲染和保存多出口字段）。 */
        private String outletSchema;

        /** 画布坐标 X。 */
        private Double x;

        /** 画布坐标 Y。 */
        private Double y;
    }
}
