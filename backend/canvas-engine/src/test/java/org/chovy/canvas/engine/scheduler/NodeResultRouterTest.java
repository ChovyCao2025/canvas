package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NodeResultRouterTest {

    private final NodeResultRouter router = new NodeResultRouter();

    @Test
    void nextNodeIdsDeduplicatesResolvedTargetsInOrder() {
        Map<String, String> routes = new LinkedHashMap<>();
        routes.put("primary", "node-a");
        routes.put("duplicate", "node-a");
        routes.put(MapFieldKeys.ELSE, "node-b");
        NodeResult result = new NodeResult(
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                true,
                null,
                false,
                null,
                routes,
                null,
                null,
                null);

        assertThat(router.nextNodeIds(result)).containsExactly("node-a", "node-b");
    }

    @Test
    void failureAwareDownstreamOnlyReturnsConvergenceNodes() {
        DagGraph graph = graph(
                List.of(
                        node("source", "TEST", Map.of()),
                        node("hub", NodeType.HUB, Map.of()),
                        node("aggregate", NodeType.AGGREGATE, Map.of()),
                        node("threshold", NodeType.THRESHOLD, Map.of()),
                        node("normal", NodeType.SEND_MESSAGE, Map.of())
                ),
                Map.of("source", List.of("hub", "aggregate", "threshold", "normal")));

        assertThat(router.failureAwareDownstream(graph, "source"))
                .containsExactly("hub", "aggregate", "threshold");
    }

    @Test
    void marksUntakenIfBranchSkippedAndPropagatesWhenAllUpstreamSkipped() {
        DagGraph graph = graph(
                List.of(
                        node("if", NodeType.IF_CONDITION, Map.of(
                                MapFieldKeys.SUCCESS_NODE_ID, "success",
                                MapFieldKeys.FAIL_NODE_ID, "fail"
                        )),
                        node("success", "TEST", Map.of()),
                        node("fail", "TEST", Map.of()),
                        node("after-fail", "TEST", Map.of()),
                        node("join", NodeType.HUB, Map.of())
                ),
                Map.of(
                        "if", List.of("success", "fail"),
                        "fail", List.of("after-fail"),
                        "after-fail", List.of("join"),
                        "success", List.of("join")
                ));
        ExecutionContext ctx = new ExecutionContext();

        router.markNonTakenBranchesSkipped(graph, "if", NodeType.IF_CONDITION,
                NodeResult.ifResult(true, "success", "fail"), ctx);

        assertThat(ctx.getNodeStatus("fail")).isEqualTo(NodeStatus.SKIPPED);
        assertThat(ctx.getNodeStatus("after-fail")).isEqualTo(NodeStatus.SKIPPED);
        assertThat(ctx.getNodeStatus("join")).isEqualTo(NodeStatus.PENDING);
    }

    private DagGraph graph(List<DagParser.CanvasNode> nodes, Map<String, List<String>> edges) {
        Map<String, DagParser.CanvasNode> nodeMap = new LinkedHashMap<>();
        Map<String, List<String>> forward = new LinkedHashMap<>();
        Map<String, List<String>> reverse = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (DagParser.CanvasNode node : nodes) {
            nodeMap.put(node.getId(), node);
            forward.put(node.getId(), edges.getOrDefault(node.getId(), List.of()));
            reverse.put(node.getId(), List.of());
            inDegree.put(node.getId(), 0);
        }
        edges.forEach((from, targets) -> targets.forEach(target -> {
            List<String> upstream = new java.util.ArrayList<>(reverse.getOrDefault(target, List.of()));
            upstream.add(from);
            reverse.put(target, upstream);
            inDegree.put(target, upstream.size());
        }));
        return new DagGraph(nodeMap, forward, reverse, inDegree);
    }

    private DagParser.CanvasNode node(String id, String type, Map<String, Object> config) {
        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId(id);
        node.setType(type);
        node.setName(id);
        node.setConfig(config);
        node.setBizConfig(Map.of());
        return node;
    }
}
