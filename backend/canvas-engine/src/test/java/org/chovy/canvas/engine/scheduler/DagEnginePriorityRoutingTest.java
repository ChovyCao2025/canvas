package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.execution.CanvasExecutionDlqMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handler.HandlerRegistry;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.handlers.PriorityHandler;
import org.chovy.canvas.infra.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DagEnginePriorityRoutingTest {

    @Test
    void priority_continues_to_next_branch_after_failed_branch_errors() {
        TestFailHandler failHandler = new TestFailHandler();
        TestOkHandler okHandler = new TestOkHandler();
        DagEngine engine = engineWithHandlers(failHandler, okHandler);
        DagGraph graph = graph(List.of(
                priorityNode("priority", List.of(
                        Map.of("order", 1, "nextNodeId", "fail"),
                        Map.of("order", 2, "nextNodeId", "ok")
                ), null),
                node("fail", "TEST_FAIL"),
                node("ok", "TEST_OK")
        ));
        ExecutionContext ctx = context();

        Map<String, Object> result = engine.execute(graph, "priority", ctx).block();

        assertThat(result).doesNotContainKey("error");
        assertThat(ctx.getNodeStatus("priority")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(ctx.getNodeStatus("fail")).isEqualTo(NodeStatus.FAILED);
        assertThat(ctx.getNodeStatus("ok")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(failHandler.calls()).isEqualTo(1);
        assertThat(okHandler.calls()).isEqualTo(1);
    }

    @Test
    void priority_does_not_reexecute_same_node_when_fallback_matches_failed_branch() {
        TestFailHandler failHandler = new TestFailHandler();
        DagEngine engine = engineWithHandlers(failHandler, new TestOkHandler());
        DagGraph graph = graph(List.of(
                priorityNode("priority", List.of(
                        Map.of("order", 1, "nextNodeId", "fail")
                ), "fail"),
                node("fail", "TEST_FAIL")
        ));
        ExecutionContext ctx = context();

        Map<String, Object> result = engine.execute(graph, "priority", ctx).block();

        assertThat(result).doesNotContainKey("error");
        assertThat(ctx.getNodeStatus("priority")).isEqualTo(NodeStatus.PARTIAL_FAIL);
        assertThat(ctx.getNodeStatus("fail")).isEqualTo(NodeStatus.FAILED);
        assertThat(failHandler.calls()).isEqualTo(1);
    }

    private DagEngine engineWithHandlers(NodeHandler... handlers) {
        List<NodeHandler> allHandlers = new java.util.ArrayList<>();
        allHandlers.add(new PriorityHandler());
        allHandlers.addAll(List.of(handlers));
        HandlerRegistry handlerRegistry = new HandlerRegistry(allHandlers);
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
        ctx.setExecutionId("exec-priority-test");
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

    private DagParser.CanvasNode priorityNode(String id, List<Map<String, Object>> priorities, String nextNodeId) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("priorities", new java.util.ArrayList<>(priorities));
        if (nextNodeId != null) {
            config.put("nextNodeId", nextNodeId);
        }
        DagParser.CanvasNode node = node(id, "PRIORITY");
        node.setConfig(config);
        return node;
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

    @NodeHandlerType("TEST_FAIL")
    static class TestFailHandler implements NodeHandler {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            calls.incrementAndGet();
            return Mono.just(NodeResult.fail("branch failed"));
        }

        int calls() {
            return calls.get();
        }
    }

    @NodeHandlerType("TEST_OK")
    static class TestOkHandler implements NodeHandler {
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
}
