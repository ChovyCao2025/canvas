package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handler.HandlerRegistry;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DagEngineCircuitBreakerTest {

    @Test
    void circuitBreakerRejectionDoesNotRecordDownstreamFailure() {
        CountingHandler handler = new CountingHandler();
        CircuitBreakerRegistry.CircuitBreaker breaker = mock(CircuitBreakerRegistry.CircuitBreaker.class);
        doThrow(new CircuitBreakerRegistry.CircuitBreakerOpenException("open"))
                .when(breaker).checkState();
        DagEngine engine = engine(handler, breaker);

        assertThatThrownBy(() -> engine.execute(graph(node("blocked", "TEST_BLOCKED")), "blocked", context()).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("open");

        assertThat(handler.calls()).isZero();
        verify(breaker, never()).recordFailure();
        verify(breaker, never()).recordSuccess();
    }

    @Test
    void successBookkeepingErrorDoesNotRecordSyntheticFailure() {
        CountingHandler handler = new CountingHandler();
        CircuitBreakerRegistry.CircuitBreaker breaker = mock(CircuitBreakerRegistry.CircuitBreaker.class);
        doThrow(new RuntimeException("redis down")).when(breaker).recordSuccess();
        DagEngine engine = engine(handler, breaker);

        assertThatThrownBy(() -> engine.execute(graph(node("success", "TEST_BLOCKED")), "success", context()).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("redis down");

        assertThat(handler.calls()).isEqualTo(1);
        verify(breaker).recordSuccess();
        verify(breaker, never()).recordFailure();
    }

    @Test
    void failureBookkeepingErrorIsNotRecordedTwice() {
        FailingHandler handler = new FailingHandler();
        CircuitBreakerRegistry.CircuitBreaker breaker = mock(CircuitBreakerRegistry.CircuitBreaker.class);
        doThrow(new RuntimeException("redis down")).when(breaker).recordFailure();
        DagEngine engine = engine(handler, breaker);

        assertThatThrownBy(() -> engine.execute(graph(node("fail", "TEST_FAILING")), "fail", context()).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("redis down");

        verify(breaker, times(1)).recordFailure();
        verify(breaker, never()).recordSuccess();
    }

    private DagEngine engine(NodeHandler handler, CircuitBreakerRegistry.CircuitBreaker breaker) {
        HandlerRegistry handlerRegistry = new HandlerRegistry(List.of(handler));
        ReflectionTestUtils.invokeMethod(handlerRegistry, "init");

        CircuitBreakerRegistry cbRegistry = mock(CircuitBreakerRegistry.class);
        when(cbRegistry.get(anyString())).thenReturn(breaker);

        DagEngine engine = new DagEngine(
                handlerRegistry,
                mock(TraceWriteBuffer.class),
                mock(CanvasExecutionDlqMapper.class),
                cbRegistry,
                mock(CanvasMetrics.class),
                new ObjectMapper(),
                mock(ContextPersistenceService.class),
                mock(org.chovy.canvas.engine.trigger.CanvasExecutionService.class)
        );
        ReflectionTestUtils.setField(engine, "maxRetry", 1);
        ReflectionTestUtils.setField(engine, "retryBaseDelayMs", 1L);
        ReflectionTestUtils.setField(engine, "retryMaxDelayMs", 10L);
        return engine;
    }

    private ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-circuit-test");
        ctx.setCanvasId(1L);
        ctx.setVersionId(1L);
        ctx.setUserId("user-1");
        ctx.setTriggerType("DIRECT_CALL");
        return ctx;
    }

    private DagGraph graph(DagParser.CanvasNode node) {
        Map<String, DagParser.CanvasNode> nodeMap = new LinkedHashMap<>();
        nodeMap.put(node.getId(), node);
        return new DagGraph(nodeMap,
                Map.of(node.getId(), List.of()),
                Map.of(node.getId(), List.of()),
                Map.of(node.getId(), 0));
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

    @NodeHandlerType("TEST_BLOCKED")
    static class CountingHandler implements NodeHandler {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            calls.incrementAndGet();
            return Mono.just(NodeResult.ok(null, Map.of()));
        }

        int calls() {
            return calls.get();
        }
    }

    @NodeHandlerType("TEST_FAILING")
    static class FailingHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.fail("handler failed"));
        }
    }
}
