package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
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
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Dag Engine Pending 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class DagEnginePendingTest {

    @Test
    void pendingExecutionDoesNotMarkUnreachedNodesSkipped() {
        DagEngine engine = engineWithHandlers(new TestPendingHandler());
        DagGraph graph = graph(List.of(
                node("wait", "TEST_PENDING"),
                node("next", "TEST_NEXT")
        ));
        ExecutionContext ctx = context();

        engine.execute(graph, "wait", ctx).block();

        assertThat(ctx.getNodeStatus("wait")).isEqualTo(NodeStatus.WAITING);
        assertThat(ctx.getNodeStatus("next")).isEqualTo(NodeStatus.PENDING);
    }

    @Test
    void pendingRedisRepeatDoesNotReenterNonThresholdHandler() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        when(ctxStore.releaseNodeGate(eq("exec-pending-test"), eq("wait"), anyString()))
                .thenReturn(true);
        CountingPendingHandler handler = new CountingPendingHandler();
        DagEngine engine = engineWithHandlers(ctxStore, handler);
        DagGraph graph = graph(List.of(node("wait", "TEST_COUNTING_PENDING")));
        ExecutionContext ctx = context();

        Map<String, Object> result = engine.execute(graph, "wait", ctx).block();

        assertThat(result)
                .containsEntry(MapFieldKeys.PENDING, true)
                .containsEntry(MapFieldKeys.OUTCOME, "PENDING");
        assertThat(handler.calls()).isEqualTo(1);
        assertThat(ctx.getNodeStatus("wait")).isEqualTo(NodeStatus.WAITING);
    }

    @Test
    void pendingLocalRepeatDoesNotReenterNonThresholdHandlerBeforePersistence() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        LocalRepeatPendingHandler handler = new LocalRepeatPendingHandler();
        DagEngine engine = engineWithHandlers(ctxStore, handler);
        DagGraph graph = graph(List.of(node("wait", "TEST_LOCAL_REPEAT_PENDING")));
        ExecutionContext ctx = context();

        Map<String, Object> result = engine.execute(graph, "wait", ctx).block();

        assertThat(result)
                .containsEntry(MapFieldKeys.PENDING, true)
                .containsEntry(MapFieldKeys.OUTCOME, "PENDING");
        assertThat(handler.calls()).isEqualTo(1);
        assertThat(ctx.getNodeStatus("wait")).isEqualTo(NodeStatus.WAITING);
        verify(ctxStore).saveNodeState(eq("exec-pending-test"), eq("wait"), eq(NodeStatus.WAITING), any());
    }

    @Test
    void terminalLocalRepeatDoesNotReexecuteHandlerBeforePersistence() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        LocalRepeatSuccessHandler handler = new LocalRepeatSuccessHandler();
        DagEngine engine = engineWithHandlers(ctxStore, handler);
        DagGraph graph = graph(List.of(node("success", "TEST_LOCAL_REPEAT_SUCCESS")));
        ExecutionContext ctx = context();

        Map<String, Object> result = engine.execute(graph, "success", ctx).block();

        assertThat(result).containsEntry("ok", true);
        assertThat(handler.calls()).isEqualTo(1);
        assertThat(ctx.getNodeStatus("success")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(ctx.getNodeOutputs().get("success")).containsEntry("call", 1);
        verify(ctxStore).saveNodeState(eq("exec-pending-test"), eq("success"),
                eq(NodeStatus.SUCCESS), eq(Map.of("ok", true, "call", 1)));
    }

    @Test
    void logicRelationImmediateFailurePersistsFailedNodeState() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        DagEngine engine = engineWithHandlers(ctxStore);
        DagGraph graph = graphWithEdge(
                node("failed-upstream", "TEST_UPSTREAM"),
                logicRelationNode("logic"),
                "failed-upstream",
                "logic");
        ExecutionContext ctx = context();
        ctx.setNodeStatus("failed-upstream", NodeStatus.FAILED);

        assertThatThrownBy(() -> engine.execute(graph, "logic", ctx).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("LOGIC_RELATION AND 条件因上游失败不可满足");

        verify(ctxStore).saveNodeState("exec-pending-test", "logic", NodeStatus.FAILED, Map.of());
    }

    @Test
    void logicRelationImmediateFailureNodeStatePersistenceFailureFailsClosed() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        doThrow(new RuntimeException("redis down"))
                .when(ctxStore).saveNodeState("exec-pending-test", "logic", NodeStatus.FAILED, Map.of());
        DagEngine engine = engineWithHandlers(ctxStore);
        DagGraph graph = graphWithEdge(
                node("failed-upstream", "TEST_UPSTREAM"),
                logicRelationNode("logic"),
                "failed-upstream",
                "logic");
        ExecutionContext ctx = context();
        ctx.setNodeStatus("failed-upstream", NodeStatus.FAILED);

        assertThatThrownBy(() -> engine.execute(graph, "logic", ctx).block())
                .isInstanceOf(NodeStatePersistenceException.class)
                .hasMessageContaining("Failed to persist node state before releasing Redis gate");

        assertThat(ctx.getGate("logic").executing.get()).isFalse();
        verify(ctxStore).saveNodeState("exec-pending-test", "logic", NodeStatus.FAILED, Map.of());
        verify(ctxStore, never()).save(ctx);
    }

    @Test
    void nodeStatePersistenceFailureRetainsRedisGateAndFailsClosed() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        doThrow(new RuntimeException("redis down"))
                .when(ctxStore).saveNodeState(anyString(), anyString(), any(NodeStatus.class), any());
        DagEngine engine = engineWithHandlers(ctxStore, new TestSuccessHandler());
        DagGraph graph = graph(List.of(node("success", "TEST_SUCCESS")));
        ExecutionContext ctx = context();

        assertThatThrownBy(() -> engine.execute(graph, "success", ctx).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to persist node state before releasing Redis gate");

        assertThat(ctx.getNodeStatus("success")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(ctx.getGate("success").executing.get()).isFalse();
        verify(ctxStore).saveNodeState(eq("exec-pending-test"), eq("success"),
                eq(NodeStatus.SUCCESS), eq(Map.of("ok", true)));
        verify(ctxStore, never()).releaseNodeGate(eq("exec-pending-test"), eq("success"), anyString());
        verify(ctxStore, never()).save(ctx);
    }

    @Test
    void errorPathNodeStatePersistenceFailureRetainsRedisGateAndFailsClosed() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        doThrow(new RuntimeException("redis down"))
                .when(ctxStore).saveNodeState(eq("exec-pending-test"), eq("huge"),
                        eq(NodeStatus.FAILED), any());
        DagEngine engine = engineWithHandlers(ctxStore, new HugeOutputHandler());
        DagGraph graph = graph(List.of(node("huge", "TEST_HUGE_OUTPUT")));
        ExecutionContext ctx = context();

        assertThatThrownBy(() -> engine.execute(graph, "huge", ctx).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to persist node state before releasing Redis gate");

        assertThat(ctx.getNodeStatus("huge")).isEqualTo(NodeStatus.FAILED);
        assertThat(ctx.getGate("huge").executing.get()).isFalse();
        verify(ctxStore).saveNodeState(eq("exec-pending-test"), eq("huge"), eq(NodeStatus.FAILED), eq(Map.of()));
        verify(ctxStore, never()).releaseNodeGate(eq("exec-pending-test"), eq("huge"), anyString());
        verify(ctxStore, never()).save(ctx);
    }

    @Test
    void synchronousErrorAfterRedisGatePersistsFailureBeforeRelease() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        TraceWriteBuffer traceBuffer = mock(TraceWriteBuffer.class);
        doThrow(new RuntimeException("trace down"))
                .when(traceBuffer).offer(any(CanvasExecutionTraceDO.class));
        DagEngine engine = engineWithHandlers(ctxStore, traceBuffer, new TestSuccessHandler());
        DagGraph graph = graph(List.of(node("success", "TEST_SUCCESS")));
        ExecutionContext ctx = context();

        assertThatThrownBy(() -> engine.execute(graph, "success", ctx).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("trace down");

        assertThat(ctx.getNodeStatus("success")).isEqualTo(NodeStatus.FAILED);
        InOrder inOrder = inOrder(ctxStore);
        inOrder.verify(ctxStore).saveNodeState("exec-pending-test", "success", NodeStatus.FAILED, Map.of());
        inOrder.verify(ctxStore).releaseNodeGate(eq("exec-pending-test"), eq("success"), anyString());
    }

    @Test
    void traceEndFailureAfterSuccessPersistsNodeStateBeforeRelease() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        TraceWriteBuffer traceBuffer = traceBufferFailingOnSecondOffer();
        DagEngine engine = engineWithHandlers(ctxStore, traceBuffer, new TestSuccessHandler());
        DagGraph graph = graph(List.of(node("success", "TEST_SUCCESS")));
        ExecutionContext ctx = context();

        assertThatThrownBy(() -> engine.execute(graph, "success", ctx).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("trace end down");

        assertThat(ctx.getNodeStatus("success")).isEqualTo(NodeStatus.SUCCESS);
        InOrder inOrder = inOrder(ctxStore);
        inOrder.verify(ctxStore).saveNodeState("exec-pending-test", "success",
                NodeStatus.SUCCESS, Map.of("ok", true));
        inOrder.verify(ctxStore).releaseNodeGate(eq("exec-pending-test"), eq("success"), anyString());
    }

    @Test
    void traceEndFailureAfterFailurePersistsNodeStateBeforeRelease() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        TraceWriteBuffer traceBuffer = traceBufferFailingOnSecondOffer();
        DagEngine engine = engineWithHandlers(ctxStore, traceBuffer, new TestFailHandler());
        DagGraph graph = graph(List.of(node("fail", "TEST_FAIL")));
        ExecutionContext ctx = context();

        assertThatThrownBy(() -> engine.execute(graph, "fail", ctx).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("trace end down");

        assertThat(ctx.getNodeStatus("fail")).isEqualTo(NodeStatus.FAILED);
        InOrder inOrder = inOrder(ctxStore);
        inOrder.verify(ctxStore).saveNodeState("exec-pending-test", "fail", NodeStatus.FAILED, Map.of());
        inOrder.verify(ctxStore).releaseNodeGate(eq("exec-pending-test"), eq("fail"), anyString());
    }

    @Test
    void finalSkippedNodesArePersistedIncrementally() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        DagEngine engine = engineWithHandlers(ctxStore, new TestSuccessHandler());
        DagGraph graph = graph(List.of(
                node("success", "TEST_SUCCESS"),
                node("unreachable", "TEST_UNREACHED")
        ));
        ExecutionContext ctx = context();

        engine.execute(graph, "success", ctx).block();

        assertThat(ctx.getNodeStatus("unreachable")).isEqualTo(NodeStatus.SKIPPED);
        verify(ctxStore).saveNodeState("exec-pending-test", "unreachable", NodeStatus.SKIPPED, Map.of());
    }

    @Test
    void redisNodeGateDenialSkipsHandlerAndReleasesLocalGate() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        CountingSuccessHandler handler = new CountingSuccessHandler();
        DagEngine engine = engineWithHandlers(ctxStore, handler);
        when(ctxStore.tryAcquireNodeGate(eq("exec-pending-test"), eq("success"),
                anyString(), eq(java.time.Duration.ofSeconds(600))))
                .thenReturn(false)
                .thenReturn(true);
        DagGraph graph = graph(List.of(node("success", "TEST_COUNTING_SUCCESS")));
        ExecutionContext ctx = context();

        Map<String, Object> denied = engine.execute(graph, "success", ctx).block();
        assertThat(denied).isEmpty();
        assertThat(handler.calls()).isZero();
        assertThat(ctx.getGate("success").executing.get()).isFalse();
        assertThat(ctx.getGate("success").repeatPending.get()).isFalse();
        verify(ctxStore, never()).releaseNodeGate(eq("exec-pending-test"), eq("success"), anyString());

        clearInvocations(ctxStore);
        Map<String, Object> allowed = engine.execute(graph, "success", ctx).block();

        assertThat(allowed).containsEntry("ok", true);
        assertThat(handler.calls()).isEqualTo(1);
        assertThat(ctx.getGate("success").executing.get()).isFalse();
        assertThat(ctx.getGate("success").repeatPending.get()).isFalse();
        verify(ctxStore).releaseNodeGate(eq("exec-pending-test"), eq("success"), anyString());
    }

    private DagEngine engineWithHandlers(NodeHandler... handlers) {
        return engineWithHandlers(mock(ContextPersistenceService.class), handlers);
    }

    private DagEngine engineWithHandlers(ContextPersistenceService ctxStore, NodeHandler... handlers) {
        return engineWithHandlers(ctxStore, mock(TraceWriteBuffer.class), handlers);
    }

    private DagEngine engineWithHandlers(ContextPersistenceService ctxStore,
                                         TraceWriteBuffer traceBuffer,
                                         NodeHandler... handlers) {
        when(ctxStore.tryAcquireNodeGate(anyString(), anyString(), anyString(), any())).thenReturn(true);
        HandlerRegistry handlerRegistry = new HandlerRegistry(List.of(handlers));
        ReflectionTestUtils.invokeMethod(handlerRegistry, "init");

        CircuitBreakerRegistry cbRegistry = mock(CircuitBreakerRegistry.class);
        when(cbRegistry.get(anyString())).thenAnswer(invocation ->
                new CircuitBreakerRegistry.CircuitBreaker(invocation.getArgument(0), 100, 30, 3));

        DagEngine engine = new DagEngine(
                handlerRegistry,
                traceBuffer,
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

    private TraceWriteBuffer traceBufferFailingOnSecondOffer() {
        TraceWriteBuffer traceBuffer = mock(TraceWriteBuffer.class);
        doNothing()
                .doThrow(new RuntimeException("trace end down"))
                .when(traceBuffer).offer(any(CanvasExecutionTraceDO.class));
        return traceBuffer;
    }

    private ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-pending-test");
        ctx.setCanvasId(1L);
        ctx.setVersionId(1L);
        ctx.setUserId("user-1");
        ctx.setTriggerType("DIRECT_CALL");
        return ctx;
    }

    private DagGraph graph(List<DagParser.CanvasNode> nodes) {
        Map<String, DagParser.CanvasNode> nodeMap = new LinkedHashMap<>();
        Map<String, List<String>> forward = new LinkedHashMap<>();
        Map<String, List<String>> reverse = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (DagParser.CanvasNode node : nodes) {
            nodeMap.put(node.getId(), node);
            forward.put(node.getId(), List.of());
            reverse.put(node.getId(), List.of());
            inDegree.put(node.getId(), 0);
        }
        return new DagGraph(nodeMap, forward, reverse, inDegree);
    }

    private DagGraph graphWithEdge(DagParser.CanvasNode upstream, DagParser.CanvasNode downstream,
                                   String upstreamId, String downstreamId) {
        Map<String, DagParser.CanvasNode> nodeMap = new LinkedHashMap<>();
        nodeMap.put(upstream.getId(), upstream);
        nodeMap.put(downstream.getId(), downstream);
        Map<String, List<String>> forward = new LinkedHashMap<>();
        forward.put(upstreamId, List.of(downstreamId));
        forward.put(downstreamId, List.of());
        Map<String, List<String>> reverse = new LinkedHashMap<>();
        reverse.put(upstreamId, List.of());
        reverse.put(downstreamId, List.of(upstreamId));
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        inDegree.put(upstreamId, 0);
        inDegree.put(downstreamId, 1);
        return new DagGraph(nodeMap, forward, reverse, inDegree);
    }

    private DagParser.CanvasNode node(String id, String type) {
        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId(id);
        node.setType(type);
        node.setName(id);
        node.setConfig(Map.of());
        node.setBizConfig(Map.of());
        return node;
    }

    private DagParser.CanvasNode logicRelationNode(String id) {
        DagParser.CanvasNode node = node(id, "LOGIC_RELATION");
        node.setConfig(Map.of("relation", "AND"));
        return node;
    }

    @NodeHandlerType("TEST_PENDING")
    static class TestPendingHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.pending(1_785_000_000_000L, "TEST_PENDING", "pending"));
        }
    }

    @NodeHandlerType("TEST_COUNTING_PENDING")
    static class CountingPendingHandler implements NodeHandler {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            calls.incrementAndGet();
            return Mono.just(NodeResult.pending(1_785_000_000_000L, "TEST_PENDING", "pending"));
        }

        int calls() {
            return calls.get();
        }
    }

    @NodeHandlerType("TEST_LOCAL_REPEAT_PENDING")
    static class LocalRepeatPendingHandler implements NodeHandler {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            int call = calls.incrementAndGet();
            if (call == 1) {
                ctx.getGate("wait").repeatPending.set(true);
            }
            return Mono.just(NodeResult.pending(1_785_000_000_000L, "TEST_PENDING", "pending"));
        }

        int calls() {
            return calls.get();
        }
    }

    @NodeHandlerType("TEST_SUCCESS")
    static class TestSuccessHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.terminal(Map.of("ok", true)));
        }
    }

    @NodeHandlerType("TEST_COUNTING_SUCCESS")
    static class CountingSuccessHandler implements NodeHandler {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            calls.incrementAndGet();
            return Mono.just(NodeResult.terminal(Map.of("ok", true)));
        }

        int calls() {
            return calls.get();
        }
    }

    @NodeHandlerType("TEST_LOCAL_REPEAT_SUCCESS")
    static class LocalRepeatSuccessHandler implements NodeHandler {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            int call = calls.incrementAndGet();
            if (call == 1) {
                ctx.getGate("success").repeatPending.set(true);
            }
            return Mono.just(NodeResult.terminal(Map.of("ok", true, "call", call)));
        }

        int calls() {
            return calls.get();
        }
    }

    @NodeHandlerType("TEST_HUGE_OUTPUT")
    static class HugeOutputHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.terminal(Map.of("huge", "x".repeat(2 * 1024 * 1024))));
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
