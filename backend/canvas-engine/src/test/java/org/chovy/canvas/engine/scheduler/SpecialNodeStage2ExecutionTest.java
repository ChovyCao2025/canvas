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
import org.chovy.canvas.engine.handlers.GroovyScriptCache;
import org.chovy.canvas.engine.handlers.GroovyHandler;
import org.chovy.canvas.engine.handlers.AggregateHandler;
import org.chovy.canvas.engine.handlers.HubHandler;
import org.chovy.canvas.engine.handlers.LogicRelationHandler;
import org.chovy.canvas.engine.handlers.MergeHandler;
import org.chovy.canvas.engine.handlers.ThresholdHandler;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Special Node Stage 2 Execution 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class SpecialNodeStage2ExecutionTest {

    @Test
    void hubExecutesOnceAfterAllUpstreamDone() {
        RecordingHubHandler hubHandler = new RecordingHubHandler();
        DagEngine engine = engineWithHandlers(hubHandler);
        DagGraph graph = graph(List.of(
                node("upA", "TEST_SOURCE"),
                node("upB", "TEST_SOURCE"),
                node("hub", NodeType.HUB)
        ), Map.of(
                "upA", List.of("hub"),
                "upB", List.of("hub")
        ));
        ExecutionContext ctx = context();

        ctx.setNodeStatus("upA", NodeStatus.SUCCESS);
        engine.execute(graph, "hub", ctx).block();

        assertThat(ctx.getNodeStatus("hub")).isEqualTo(NodeStatus.WAITING);
        assertThat(hubHandler.calls()).isZero();

        ctx.setNodeStatus("upB", NodeStatus.SUCCESS);
        engine.execute(graph, "hub", ctx).block();

        assertThat(ctx.getNodeStatus("hub")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(hubHandler.calls()).isEqualTo(1);
    }

    @Test
    void aggregateReceivesInjectedUpstreamIdsAfterAllUpstreamDone() {
        DagEngine engine = engineWithHandlers();
        DagGraph graph = graph(List.of(
                node("upA", "TEST_SOURCE"),
                node("upB", "TEST_SOURCE"),
                node("aggregate", NodeType.AGGREGATE)
        ), Map.of(
                "upA", List.of("aggregate"),
                "upB", List.of("aggregate")
        ));
        ExecutionContext ctx = context();

        ctx.setNodeStatus("upA", NodeStatus.SUCCESS);
        ctx.setNodeStatus("upB", NodeStatus.SUCCESS);
        Map<String, Object> result = engine.execute(graph, "aggregate", ctx).block();

        assertThat(ctx.getNodeStatus("aggregate")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(result).containsEntry(MapFieldKeys.SUCCESS_COUNT, 2L);
        assertThat(result).containsEntry(MapFieldKeys.TOTAL_COUNT, 2L);
        assertThat(result).containsEntry(MapFieldKeys.PASSED, true);
    }

    @Test
    void thresholdTransitionsFromWaitingToSuccessAsSignalsAccumulate() {
        DagEngine engine = engineWithHandlers();
        DagParser.CanvasNode threshold = node("threshold", NodeType.THRESHOLD);
        threshold.setConfig(Map.of(
                "threshold", 2,
                "thresholdMode", "min_done",
                MapFieldKeys.SUCCESS_NODE_ID, "done"
        ));
        DagGraph graph = graph(List.of(
                node("upA", "TEST_SOURCE"),
                node("upB", "TEST_SOURCE"),
                threshold,
                node("done", "TEST_SOURCE")
        ), Map.of(
                "upA", List.of("threshold"),
                "upB", List.of("threshold"),
                "threshold", List.of("done")
        ));
        ExecutionContext ctx = context();

        ctx.setNodeStatus("upA", NodeStatus.SUCCESS);
        engine.execute(graph, "threshold", ctx).block();

        assertThat(ctx.getNodeStatus("threshold")).isEqualTo(NodeStatus.WAITING);

        ctx.setNodeStatus("upB", NodeStatus.SUCCESS);
        engine.execute(graph, "threshold", ctx).block();

        assertThat(ctx.getNodeStatus("threshold")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(ctx.getNodeStatus("done")).isEqualTo(NodeStatus.SUCCESS);
    }

    @Test
    void nodeFailurePropagatesToCallerInsteadOfReturningErrorMap() {
        DagEngine engine = engineWithHandlers(new FailingHandler());
        DagGraph graph = graph(List.of(
                node("fail", "TEST_FAIL")
        ), Map.of());
        ExecutionContext ctx = context();

        assertThatThrownBy(() -> engine.execute(graph, "fail", ctx).block())
                .hasMessageContaining("节点 fail 失败");

        assertThat(ctx.getNodeStatus("fail")).isEqualTo(NodeStatus.FAILED);
    }

    @Test
    void failedHubReentryPropagatesTerminalFailureInsteadOfReturningToWaiting() {
        DagEngine engine = engineWithHandlers();
        DagGraph graph = graph(List.of(
                node("upA", "TEST_SOURCE"),
                node("hub", NodeType.HUB)
        ), Map.of(
                "upA", List.of("hub")
        ));
        ExecutionContext ctx = context();
        ctx.setNodeStatus("hub", NodeStatus.FAILED);

        assertThatThrownBy(() -> engine.execute(graph, "hub", ctx).block())
                .hasMessageContaining("节点 hub 已处于终态: FAILED");

        assertThat(ctx.getNodeStatus("hub")).isEqualTo(NodeStatus.FAILED);
    }

    private DagEngine engineWithHandlers(NodeHandler... extraHandlers) {
        List<NodeHandler> handlers = new ArrayList<>();
        handlers.add(new HubHandler());
        handlers.add(new AggregateHandler(new GroovyHandler(mock(GroovyScriptCache.class))));
        handlers.add(new LogicRelationHandler());
        handlers.add(new ThresholdHandler());
        handlers.add(new MergeHandler());
        handlers.add(new TestSourceHandler());
        handlers.addAll(List.of(extraHandlers));

        HandlerRegistry handlerRegistry = new HandlerRegistry(handlers);
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
        ReflectionTestUtils.setField(engine, "globalTimeout", 600L);
        return engine;
    }

    private ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-special-stage2-test");
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
            forward.put(node.getId(), new ArrayList<>());
            reverse.put(node.getId(), new ArrayList<>());
            inDegree.put(node.getId(), 0);
        }
        for (Map.Entry<String, List<String>> entry : edges.entrySet()) {
            forward.get(entry.getKey()).addAll(entry.getValue());
            for (String target : entry.getValue()) {
                reverse.get(target).add(entry.getKey());
                inDegree.put(target, inDegree.getOrDefault(target, 0) + 1);
            }
        }
        return new DagGraph(nodeMap, forward, reverse, inDegree);
    }

    private DagParser.CanvasNode node(String id, String type) {
        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId(id);
        node.setType(type);
        node.setName(id);
        node.setConfig(new HashMap<>());
        node.setBizConfig(Map.of());
        return node;
    }

    @NodeHandlerType(NodeType.HUB)
    static class RecordingHubHandler implements NodeHandler {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            calls.incrementAndGet();
            return Mono.just(NodeResult.terminal(Map.of("hub", true)));
        }

        int calls() {
            return calls.get();
        }
    }

    @NodeHandlerType(NodeType.AGGREGATE)
    static class RecordingAggregateHandler implements NodeHandler {
        private final AtomicInteger calls = new AtomicInteger();
        private List<String> lastUpstreamIds = List.of();

        @Override
        @SuppressWarnings("unchecked")
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            calls.incrementAndGet();
            lastUpstreamIds = List.copyOf((List<String>) config.getOrDefault(MapFieldKeys.UPSTREAM_IDS, List.of()));
            return Mono.just(NodeResult.terminal(Map.of("aggregate", true)));
        }

        int calls() {
            return calls.get();
        }

        List<String> lastUpstreamIds() {
            return lastUpstreamIds;
        }
    }

    @NodeHandlerType("TEST_SOURCE")
    static class TestSourceHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.terminal(Map.of()));
        }
    }

    @NodeHandlerType("TEST_FAIL")
    static class FailingHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.fail("boom"));
        }
    }

    @NodeHandlerType(NodeType.IF_CONDITION)
    static class BranchHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.ifResult(true,
                    (String) config.get(MapFieldKeys.SUCCESS_NODE_ID),
                    (String) config.get(MapFieldKeys.FAIL_NODE_ID)));
        }
    }
}
