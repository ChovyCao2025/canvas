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

    List<String> nextNodeIds(NodeResult result) {
        return NodeRouteResolver.resolveTargets(result).stream()
                .distinct()
                .collect(Collectors.toList());
    }

    List<String> failureAwareDownstream(DagGraph graph, String sourceNodeId) {
        return graph.downstream(sourceNodeId).stream()
                .filter(nextId -> {
                    DagParser.CanvasNode nextNode = graph.getNode(nextId);
                    return nextNode != null && isFailureAwareConvergenceNode(nextNode.getType());
                })
                .distinct()
                .toList();
    }

    boolean isFailureAwareConvergenceNode(String nodeType) {
        return NodeType.HUB.equals(nodeType)
                || NodeType.AGGREGATE.equals(nodeType)
                || NodeType.THRESHOLD.equals(nodeType);
    }

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

    private void markSkippedPath(DagGraph graph, String nodeId, ExecutionContext ctx) {
        if (nodeId == null || nodeId.isBlank()) {
            return;
        }
        Deque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(nodeId);
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

    private boolean allUpstreamSkipped(DagGraph graph, String nodeId, ExecutionContext ctx) {
        List<String> upstreamIds = graph.upstream(nodeId);
        return !upstreamIds.isEmpty()
                && upstreamIds.stream().allMatch(upstream -> ctx.getNodeStatus(upstream) == NodeStatus.SKIPPED);
    }
}
