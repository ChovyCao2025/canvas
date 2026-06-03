package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handler.HandlerRegistry;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.handlers.HubHandler;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.infrastructure.redis.RedisDelayQueue;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpecialNodeTimeoutQueueTest {

    @Test
    void hubWaitingSchedulesRedisDelayQueueInsteadOfInMemoryTimer() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        RedisDelayQueue delayQueue = mock(RedisDelayQueue.class);
        DagEngine engine = engineWithHandlers(ctxStore, delayQueue, new HubHandler());
        DagGraph graph = graph(List.of(
                node("upstream", "TEST_SOURCE", Map.of()),
                node("hub", NodeType.HUB, Map.of("timeout", 7))
        ), Map.of("upstream", List.of("hub")));
        ExecutionContext ctx = context();

        engine.execute(graph, "hub", ctx).block();

        assertThat(ctx.getNodeStatus("hub")).isEqualTo(NodeStatus.WAITING);
        verify(ctxStore).saveNodeState("exec-timeout-test", "hub", NodeStatus.WAITING, Map.of());
        verify(delayQueue).scheduleSpecialNodeTimeout(ctx, "hub", NodeType.HUB, 7);
    }

    @Test
    void queueScheduleFailureFailsClosedInsteadOfPausingWithoutTimer() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        RedisDelayQueue delayQueue = mock(RedisDelayQueue.class);
        DagEngine engine = engineWithHandlers(ctxStore, delayQueue, new HubHandler());
        DagGraph graph = graph(List.of(
                node("upstream", "TEST_SOURCE", Map.of()),
                node("hub", NodeType.HUB, Map.of("timeout", 7))
        ), Map.of("upstream", List.of("hub")));
        ExecutionContext ctx = context();
        doThrow(new IllegalStateException("redis down"))
                .when(delayQueue).scheduleSpecialNodeTimeout(ctx, "hub", NodeType.HUB, 7);

        assertThatThrownBy(() -> engine.execute(graph, "hub", ctx).block())
                .isInstanceOf(SpecialNodeTimeoutFailureException.class)
                .hasMessageContaining("Failed to schedule special node timeout");

        assertThat(ctx.getNodeStatus("hub")).isEqualTo(NodeStatus.FAILED);
        verify(ctxStore).saveNodeState("exec-timeout-test", "hub", NodeStatus.WAITING, Map.of());
        verify(ctxStore).saveNodeState(eq("exec-timeout-test"), eq("hub"), eq(NodeStatus.FAILED), any());
    }

    @Test
    void reentryResetClearsSpecialTimeoutGeneration() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        RedisDelayQueue delayQueue = mock(RedisDelayQueue.class);
        DagEngine engine = engineWithHandlers(ctxStore, delayQueue, new HubHandler());
        DagGraph graph = graph(List.of(
                node("loop", NodeType.LOOP, Map.of()),
                node("hub", NodeType.HUB, Map.of())
        ), Map.of("loop", List.of("hub")));
        ExecutionContext ctx = context();
        ctx.setNodeStatus("hub", NodeStatus.WAITING);
        ctx.getHubStartTimes().put("hub", 1_000L);
        ctx.getScheduledHubTimeouts().add("hub");

        ReflectionTestUtils.invokeMethod(engine, "resetReachableUntilSource", graph, "hub", "loop", ctx);

        assertThat(ctx.getNodeStatus("hub")).isEqualTo(NodeStatus.PENDING);
        assertThat(ctx.getHubStartTimes()).doesNotContainKey("hub");
        assertThat(ctx.getScheduledHubTimeouts()).doesNotContain("hub");
        verify(ctxStore).deleteNodeState("exec-timeout-test", "hub");
    }

    @Test
    void staleQueuedTimeoutDoesNotHitNewWaitingGeneration() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        RedisDelayQueue delayQueue = mock(RedisDelayQueue.class);
        DagEngine engine = engineWithHandlers(ctxStore, delayQueue, new HubHandler(), new TestOkHandler());
        DagGraph graph = graph(List.of(
                node("hub", NodeType.HUB, Map.of(MapFieldKeys.TIMEOUT_NODE_ID, "target")),
                node("target", "TEST_OK", Map.of())
        ), Map.of("hub", List.of("target")));
        ExecutionContext ctx = context();
        ctx.setTriggerType(TriggerType.HUB_TIMEOUT);
        ctx.setTriggerNodeType(NodeType.HUB);
        ctx.setMatchKey("hub");
        ctx.setNodeStatus("hub", NodeStatus.WAITING);
        ctx.getHubStartTimes().put("hub", 2_000L);
        ctx.putTriggerPayloadValues(Map.of(
                MapFieldKeys.EXECUTION_ID, "exec-timeout-test",
                MapFieldKeys.VERSION_ID, 1L,
                MapFieldKeys.TIMEOUT_TIMER_KEY, "hub",
                MapFieldKeys.TIMEOUT_SCHEDULED_AT_EPOCH_MS, 1_000L,
                MapFieldKeys.TIMEOUT_FIRE_AT_EPOCH_MS, 8_000L,
                MapFieldKeys.TIMEOUT_SECONDS, 7L));

        Map<String, Object> result = engine.execute(graph, "hub", ctx).block();

        assertThat(result).containsEntry(MapFieldKeys.SKIPPED, "stale-timeout-payload");
        assertThat(ctx.getNodeStatus("hub")).isEqualTo(NodeStatus.WAITING);
        assertThat(ctx.getNodeStatus("target")).isEqualTo(NodeStatus.PENDING);
    }

    @Test
    void specialTimeoutTriggerExecutesTimeoutBranchThroughDagEngine() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        RedisDelayQueue delayQueue = mock(RedisDelayQueue.class);
        DagEngine engine = engineWithHandlers(ctxStore, delayQueue, new HubHandler(), new TestOkHandler());
        DagGraph graph = graph(List.of(
                node("hub", NodeType.HUB, Map.of(MapFieldKeys.TIMEOUT_NODE_ID, "target")),
                node("target", "TEST_OK", Map.of())
        ), Map.of("hub", List.of("target")));
        ExecutionContext ctx = context();
        ctx.setTriggerType(TriggerType.HUB_TIMEOUT);
        ctx.setTriggerNodeType(NodeType.HUB);
        ctx.setMatchKey("hub");
        ctx.setNodeStatus("hub", NodeStatus.WAITING);
        ctx.getHubStartTimes().put("hub", 1_000L);
        ctx.putTriggerPayloadValues(Map.of(
                MapFieldKeys.EXECUTION_ID, "exec-timeout-test",
                MapFieldKeys.VERSION_ID, 1L,
                MapFieldKeys.TIMEOUT_TIMER_KEY, "hub",
                MapFieldKeys.TIMEOUT_SCHEDULED_AT_EPOCH_MS, 1_000L,
                MapFieldKeys.TIMEOUT_FIRE_AT_EPOCH_MS, 8_000L,
                MapFieldKeys.TIMEOUT_SECONDS, 7L));

        Map<String, Object> result = engine.execute(graph, "hub", ctx).block();

        assertThat(result).containsEntry("ok", true);
        assertThat(ctx.getNodeStatus("hub")).isEqualTo(NodeStatus.TIMEOUT);
        assertThat(ctx.getNodeStatus("target")).isEqualTo(NodeStatus.SUCCESS);
        verify(ctxStore).saveNodeState(eq("exec-timeout-test"), eq("hub"),
                eq(NodeStatus.TIMEOUT), any());
        verify(delayQueue, never()).scheduleSpecialNodeTimeout(any(), anyString(), anyString(), anyLong());
    }

    private DagEngine engineWithHandlers(ContextPersistenceService ctxStore,
                                         RedisDelayQueue delayQueue,
                                         NodeHandler... handlers) {
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
                delayQueue
        );
        ReflectionTestUtils.setField(engine, "maxRetry", 1);
        ReflectionTestUtils.setField(engine, "retryBaseDelayMs", 1L);
        ReflectionTestUtils.setField(engine, "retryMaxDelayMs", 10L);
        return engine;
    }

    private ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-timeout-test");
        ctx.setCanvasId(10L);
        ctx.setVersionId(1L);
        ctx.setUserId("user-1");
        ctx.setTriggerType(TriggerType.DIRECT_CALL);
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

    @NodeHandlerType("TEST_OK")
    static class TestOkHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.terminal(Map.of("ok", true)));
        }
    }
}
