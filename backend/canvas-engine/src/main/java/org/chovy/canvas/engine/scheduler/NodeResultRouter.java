package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.handler.NodeRouteResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves DAG routing decisions from node results and current graph state.
 */
@Component
public class NodeResultRouter {

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param result result 参数，用于 nextNodeIds 流程中的校验、计算或对象转换。
     * @return 返回 next node ids 汇总后的集合、分页或映射视图。
     */
    List<String> nextNodeIds(NodeResult result) {
        return NodeRouteResolver.resolveTargets(result).stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param graph graph 参数，用于 failureAwareDownstream 流程中的校验、计算或对象转换。
     * @param sourceNodeId 业务对象 ID，用于定位具体记录。
     * @return 返回 failure aware downstream 汇总后的集合、分页或映射视图。
     */
    List<String> failureAwareDownstream(DagGraph graph, String sourceNodeId) {
        return graph.downstream(sourceNodeId).stream()
                .filter(nextId -> {
                    DagParser.CanvasNode nextNode = graph.getNode(nextId);
                    return nextNode != null && isFailureAwareConvergenceNode(nextNode.getType());
                })
                .distinct()
                .toList();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param nodeType 类型标识，用于选择对应处理分支。
     * @return 返回布尔判断结果。
     */
    boolean isFailureAwareConvergenceNode(String nodeType) {
        return NodeType.HUB.equals(nodeType)
                || NodeType.AGGREGATE.equals(nodeType)
                || NodeType.THRESHOLD.equals(nodeType);
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param graph graph 参数，用于 markNonTakenBranchesSkipped 流程中的校验、计算或对象转换。
     * @param sourceNodeId 业务对象 ID，用于定位具体记录。
     * @param sourceType 类型标识，用于选择对应处理分支。
     * @param result result 参数，用于 markNonTakenBranchesSkipped 流程中的校验、计算或对象转换。
     * @param ctx ctx 参数，用于 markNonTakenBranchesSkipped 流程中的校验、计算或对象转换。
     */
    void markNonTakenBranchesSkipped(DagGraph graph,
                                     String sourceNodeId,
                                     String sourceType,
                                     NodeResult result,
                                     ExecutionContext ctx) {
        DagParser.CanvasNode node = graph.getNode(sourceNodeId);
        if (node == null || node.getConfig() == null) return;
        Map<String, Object> cfg = node.getConfig();

        if (NodeType.IF_CONDITION.equals(sourceType)) {
            String successId = (String) cfg.get(MapFieldKeys.SUCCESS_NODE_ID);
            String failId = (String) cfg.get(MapFieldKeys.FAIL_NODE_ID);
            String takenId = result.successNodeId() != null ? result.successNodeId() : result.failNodeId();
            String skippedId = takenId != null && takenId.equals(successId) ? failId : successId;
            markSkippedPath(graph, skippedId, ctx);
        }
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param graph graph 参数，用于 markSkippedPath 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param ctx ctx 参数，用于 markSkippedPath 流程中的校验、计算或对象转换。
     */
    private void markSkippedPath(DagGraph graph, String nodeId, ExecutionContext ctx) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (nodeId == null || nodeId.isBlank()) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        Deque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(nodeId);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            ctx.setNodeStatusIfNotDone(current, NodeStatus.SKIPPED);
            for (String downstream : graph.downstream(current)) {
                if (allUpstreamSkipped(graph, downstream, ctx)) {
                    queue.addLast(downstream);
                }
            }
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param graph graph 参数，用于 allUpstreamSkipped 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param ctx ctx 参数，用于 allUpstreamSkipped 流程中的校验、计算或对象转换。
     * @return 返回 all upstream skipped 的布尔判断结果。
     */
    private boolean allUpstreamSkipped(DagGraph graph, String nodeId, ExecutionContext ctx) {
        List<String> upstreamIds = graph.upstream(nodeId);
        return !upstreamIds.isEmpty()
                && upstreamIds.stream().allMatch(upstream -> ctx.getNodeStatus(upstream) == NodeStatus.SKIPPED);
    }
}
