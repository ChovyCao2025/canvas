package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handler.HandlerRegistry;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DagEngineCommitActionTest {

    @Test
    void couponReceivesNodeIdForNodeScopedIdempotencyKey() {
        DagEngine engine = engineWithHandlers(new TestCouponHandler());
        DagGraph graph = graph(List.of(
                node("coupon-a", NodeType.COUPON, Map.of())
        ), Map.of());
        ExecutionContext ctx = context();

        engine.execute(graph, "coupon-a", ctx).block();

        assertThat(ctx.getNodeOutputs().get("coupon-a"))
                .containsEntry("seenNodeId", "coupon-a");
    }

    @Test
    void commitActionSuccessMakesLaterFailureOverallSuccessful() {
        DagEngine engine = engineWithHandlers(new TestCommitActionHandler(), new TestFailHandler());
        DagGraph graph = graph(List.of(
                node("commit", "COMMIT_ACTION", Map.of(MapFieldKeys.NEXT_NODE_ID, "fail")),
                node("fail", "TEST_FAIL", Map.of())
        ), Map.of("commit", List.of("fail")));
        ExecutionContext ctx = context();

        Map<String, Object> result = engine.execute(graph, "commit", ctx).block();

        assertThat(result).doesNotContainKey(MapFieldKeys.ERROR);
        assertThat(ctx.isBenefitGranted()).isTrue();
        assertThat(ctx.getNodeStatus("commit")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(ctx.getNodeStatus("fail")).isEqualTo(NodeStatus.FAILED);
    }

    @Test
    void downstreamFailureDoesNotOverwriteUpstreamSuccessfulNodeState() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        DagEngine engine = engineWithHandlers(ctxStore, new TestOkHandler(), new TestFailHandler());
        DagGraph graph = graph(List.of(
                node("ok", "TEST_OK", Map.of(MapFieldKeys.NEXT_NODE_ID, "fail")),
                node("fail", "TEST_FAIL", Map.of())
        ), Map.of("ok", List.of("fail")));
        ExecutionContext ctx = context();

        assertThatThrownBy(() -> engine.execute(graph, "ok", ctx).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("节点 fail 失败");

        assertThat(ctx.getNodeStatus("ok")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(ctx.getNodeOutputs().get("ok")).containsEntry("ok", true);
        verify(ctxStore).saveNodeState("exec-commit-action-test", "ok",
                NodeStatus.SUCCESS, Map.of("ok", true));
        verify(ctxStore, never()).saveNodeState(eq("exec-commit-action-test"), eq("ok"),
                eq(NodeStatus.FAILED), eq(Map.of()));
    }

    @Test
    void gotoReentryDeletesStaleNodeStateForResetNodes() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        DagEngine engine = engineWithHandlers(ctxStore, new TestGotoHandler(), new TestOkHandler());
        DagGraph graph = graph(List.of(
                node("goto", NodeType.GOTO, Map.of("targetNodeId", "target")),
                node("target", "TEST_OK", Map.of())
        ), Map.of("target", List.of("goto")));
        ExecutionContext ctx = context();
        ctx.setNodeStatus("target", NodeStatus.SUCCESS);
        ctx.putNodeOutput("target", Map.of("stale", true));

        engine.execute(graph, "goto", ctx).block();

        assertThat(ctx.getNodeStatus("goto")).isEqualTo(NodeStatus.SUCCESS);
        verify(ctxStore).deleteNodeState("exec-commit-action-test", "target");
        verify(ctxStore, never()).deleteNodeState("exec-commit-action-test", "goto");
        verify(ctxStore, never()).saveNodeState("exec-commit-action-test", "goto", NodeStatus.SKIPPED, Map.of());
    }

    @Test
    void gotoReentryFailsClosedWhenStaleNodeStateDeleteFails() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        doThrow(new IllegalStateException("redis down"))
                .when(ctxStore).deleteNodeState("exec-commit-action-test", "target");
        DagEngine engine = engineWithHandlers(ctxStore, new TestGotoHandler(), new TestOkHandler());
        DagGraph graph = graph(List.of(
                node("goto", NodeType.GOTO, Map.of("targetNodeId", "target")),
                node("target", "TEST_OK", Map.of())
        ), Map.of("target", List.of("goto")));
        ExecutionContext ctx = context();
        ctx.setNodeStatus("target", NodeStatus.SUCCESS);
        ctx.putNodeOutput("target", Map.of("stale", true));

        assertThatThrownBy(() -> engine.execute(graph, "goto", ctx).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to delete node state before reentry reset");

        assertThat(ctx.getNodeStatus("target")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(ctx.getNodeOutputs().get("target")).containsEntry("stale", true);
        verify(ctxStore).deleteNodeState("exec-commit-action-test", "target");
        verify(ctxStore, never()).save(ctx);
        verify(ctxStore, never()).saveNodeState("exec-commit-action-test", "target",
                NodeStatus.SUCCESS, Map.of("ok", true));
    }

    @Test
    void successfulHandlerReleasesRedisNodeGate() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        DagEngine engine = engineWithHandlers(ctxStore, new TestOkHandler());
        DagGraph graph = graph(List.of(node("ok", "TEST_OK", Map.of())), Map.of());
        ExecutionContext ctx = context();

        engine.execute(graph, "ok", ctx).block();

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(ctxStore).tryAcquireNodeGate(eq("exec-commit-action-test"), eq("ok"),
                token.capture(), eq(Duration.ofSeconds(600)));
        verify(ctxStore).releaseNodeGate("exec-commit-action-test", "ok", token.getValue());
    }

    @Test
    void redisNodeGateReleaseFailureDoesNotFailSuccessfulExecution() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        doThrow(new RuntimeException("redis down"))
                .when(ctxStore).releaseNodeGate(eq("exec-commit-action-test"), eq("ok"), anyString());
        DagEngine engine = engineWithHandlers(ctxStore, new TestOkHandler());
        DagGraph graph = graph(List.of(node("ok", "TEST_OK", Map.of())), Map.of());
        ExecutionContext ctx = context();

        Map<String, Object> result = engine.execute(graph, "ok", ctx).block();

        assertThat(result).containsEntry("ok", true);
        verify(ctxStore).releaseNodeGate(eq("exec-commit-action-test"), eq("ok"), anyString());
    }

    @Test
    void missingHandlerReleasesRedisAndLocalNodeGate() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        DagEngine engine = engineWithHandlers(ctxStore);
        DagGraph graph = graph(List.of(node("missing", "TEST_MISSING", Map.of())), Map.of());
        ExecutionContext ctx = context();

        assertThatThrownBy(() -> engine.execute(graph, "missing", ctx).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未注册的节点类型");

        assertThat(ctx.getGate("missing").executing.get()).isFalse();
        verify(ctxStore).releaseNodeGate(eq("exec-commit-action-test"), eq("missing"), anyString());
    }

    private DagEngine engineWithHandlers(NodeHandler... handlers) {
        return engineWithHandlers(mock(ContextPersistenceService.class), handlers);
    }

    private DagEngine engineWithHandlers(ContextPersistenceService ctxStore, NodeHandler... handlers) {
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        HandlerRegistry handlerRegistry = new HandlerRegistry(List.of(handlers));
        ReflectionTestUtils.invokeMethod(handlerRegistry, "init");

        CircuitBreakerRegistry cbRegistry = mock(CircuitBreakerRegistry.class);
        when(cbRegistry.get(anyString())).thenAnswer(invocation ->
                new CircuitBreakerRegistry.CircuitBreaker(invocation.getArgument(0), 100, 30, 3));

        DagEngine engine = new DagEngine(
                handlerRegistry,
                mock(TraceWriteBuffer.class),
                mock(CanvasExecutionDlqMapper.class),
                cbRegistry,
                mock(CanvasMetrics.class),
                new ObjectMapper(),
                ctxStore,
                mock(org.chovy.canvas.engine.trigger.CanvasExecutionService.class)
        );
        ReflectionTestUtils.setField(engine, "maxRetry", 1);
        ReflectionTestUtils.setField(engine, "retryBaseDelayMs", 1L);
        ReflectionTestUtils.setField(engine, "retryMaxDelayMs", 10L);
        return engine;
    }

    private ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-commit-action-test");
        ctx.setCanvasId(1L);
        ctx.setVersionId(1L);
        ctx.setUserId("user-1");
        ctx.setTriggerType("DIRECT_CALL");
        return ctx;
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
            reverse.computeIfAbsent(target, ignored -> List.of());
            List<String> upstream = new java.util.ArrayList<>(reverse.get(target));
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

    @NodeHandlerType(NodeType.COUPON)
    static class TestCouponHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.ok((String) config.get(MapFieldKeys.NEXT_NODE_ID),
                    Map.of("seenNodeId", config.get(MapFieldKeys.NODE_ID_INTERNAL))));
        }
    }

    @NodeHandlerType("COMMIT_ACTION")
    static class TestCommitActionHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.ok((String) config.get(MapFieldKeys.NEXT_NODE_ID), Map.of()));
        }

        @Override
        public boolean isBenefitNode() {
            return true;
        }
    }

    @NodeHandlerType("TEST_OK")
    static class TestOkHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.ok((String) config.get(MapFieldKeys.NEXT_NODE_ID),
                    Map.of("ok", true)));
        }
    }

    @NodeHandlerType(NodeType.GOTO)
    static class TestGotoHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.routed("goto", (String) config.get("targetNodeId"),
                    Map.of("jump", true)));
        }
    }

    @NodeHandlerType("TEST_FAIL")
    static class TestFailHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.fail("boom"));
        }
    }
}
