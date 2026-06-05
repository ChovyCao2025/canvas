package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handler.HandlerRegistry;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.idempotency.NodeSideEffectIdempotencyService;
import org.chovy.canvas.engine.idempotency.NodeSideEffectRecord;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DagEngineContextCommitTest {

    @Test
    void failedHandlerDoesNotCommitNodeOutput() {
        DagEngine engine = engineWithHandlers(new FailingOutputHandler());
        ExecutionContext ctx = context();

        assertThatThrownBy(() -> engine.execute(graph(List.of(node("fail", "TEST_FAIL_OUTPUT", Map.of()))), "fail", ctx).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("boom");
        assertThat(ctx.getNodeStatus("fail")).isEqualTo(NodeStatus.FAILED);
        assertThat(ctx.getContextValue("fail.leaked")).isNull();
        assertThat(ctx.getNodeOutputs()).doesNotContainKey("fail");
    }

    @Test
    void successfulHandlerCommitsNodeOutputOnce() {
        CountingSideEffectHandler handler = new CountingSideEffectHandler();
        DagEngine engine = engineWithHandlers(handler);
        ExecutionContext ctx = context();
        DagGraph graph = graph(List.of(node("send", "TEST_SIDE_EFFECT", Map.of())));

        Map<String, Object> first = engine.execute(graph, "send", ctx).block();
        Map<String, Object> second = engine.execute(graph, "send", ctx).block();

        assertThat(first).containsEntry("messageId", "m-1");
        assertThat(second).isEmpty();
        assertThat(handler.calls).hasValue(1);
        assertThat(ctx.getContextValue("send.messageId")).isEqualTo("m-1");
        assertThat(ctx.getNodeStatus("send")).isEqualTo(NodeStatus.SUCCESS);
    }

    @Test
    void duplicateCompletedSideEffectReturnsCachedOutput() {
        CountingSideEffectHandler handler = new CountingSideEffectHandler();
        DagEngine engine = engineWithHandlers(handler);
        NodeSideEffectIdempotencyService idempotencyService = mock(NodeSideEffectIdempotencyService.class);
        NodeSideEffectRecord record = NodeSideEffectRecord.builder()
                .id(99L)
                .status(NodeSideEffectIdempotencyService.STATUS_COMPLETED)
                .build();
        when(idempotencyService.reserve(org.mockito.ArgumentMatchers.any(), anyString(), anyString(), anyString()))
                .thenReturn(new NodeSideEffectIdempotencyService.ReserveResult(
                        false, true, false, record, Map.of("messageId", "cached")));
        ReflectionTestUtils.setField(engine, "sideEffectIdempotencyService", idempotencyService);
        ExecutionContext ctx = context();

        Map<String, Object> result = engine.execute(
                graph(List.of(node("send", "TEST_SIDE_EFFECT", Map.of()))),
                "send",
                ctx).block();

        assertThat(result).containsEntry("messageId", "cached");
        assertThat(handler.calls).hasValue(0);
        assertThat(ctx.getContextValue("send.messageId")).isEqualTo("cached");
        verify(idempotencyService, never()).complete(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private DagEngine engineWithHandlers(NodeHandler... handlers) {
        HandlerRegistry handlerRegistry = new HandlerRegistry(List.of(handlers));
        ReflectionTestUtils.invokeMethod(handlerRegistry, "init");

        CircuitBreakerRegistry cbRegistry = mock(CircuitBreakerRegistry.class);
        when(cbRegistry.get(anyString())).thenAnswer(invocation ->
                new CircuitBreakerRegistry.CircuitBreaker(invocation.getArgument(0), 100, 30, 3));

        DagEngine engine = new DagEngine(
                handlerRegistry,
                mock(TraceWriteBuffer.class),
                cbRegistry,
                mock(CanvasMetrics.class),
                new ObjectMapper(),
                mock(ContextPersistenceService.class),
                mock(org.chovy.canvas.engine.trigger.CanvasExecutionService.class),
                mock(ExecutionDlqWriter.class),
                new NodeResultRouter()
        );
        ReflectionTestUtils.setField(engine, "maxRetry", 1);
        ReflectionTestUtils.setField(engine, "retryBaseDelayMs", 1L);
        ReflectionTestUtils.setField(engine, "retryMaxDelayMs", 10L);
        return engine;
    }

    private ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setTenantId(7L);
        ctx.setExecutionId("exec-context-commit-test");
        ctx.setCanvasId(42L);
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

    private DagParser.CanvasNode node(String id, String type, Map<String, Object> config) {
        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId(id);
        node.setType(type);
        node.setName(id);
        node.setConfig(config);
        node.setBizConfig(Map.of());
        return node;
    }

    @NodeHandlerType("TEST_FAIL_OUTPUT")
    static class FailingOutputHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(new NodeResult(
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of("leaked", true),
                    false,
                    "boom",
                    false,
                    NodeOutcome.FAIL,
                    Map.of(),
                    "NODE_FAILED",
                    "boom",
                    null
            ));
        }
    }

    @NodeHandlerType("TEST_SIDE_EFFECT")
    static class CountingSideEffectHandler implements NodeHandler {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            calls.incrementAndGet();
            return Mono.just(NodeResult.ok((String) config.get(MapFieldKeys.NEXT_NODE_ID),
                    Map.of("messageId", "m-1")));
        }

        @Override
        public boolean requiresSideEffectIdempotency(Map<String, Object> config, ExecutionContext ctx) {
            return true;
        }

        @Override
        public String sideEffectOperationKey(Map<String, Object> config, ExecutionContext ctx) {
            return ctx.getUserId() + ":test-side-effect";
        }
    }
}
