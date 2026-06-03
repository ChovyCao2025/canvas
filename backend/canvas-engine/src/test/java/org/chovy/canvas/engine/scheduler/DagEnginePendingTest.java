package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
    void nodeStatePersistenceFailureDoesNotAlterSuccessfulResult() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        doThrow(new RuntimeException("redis down"))
                .when(ctxStore).saveNodeState(anyString(), anyString(), any(NodeStatus.class), any());
        DagEngine engine = engineWithHandlers(ctxStore, new TestSuccessHandler());
        DagGraph graph = graph(List.of(node("success", "TEST_SUCCESS")));
        ExecutionContext ctx = context();

        Map<String, Object> result = engine.execute(graph, "success", ctx).block();

        assertThat(result).containsEntry("ok", true);
        assertThat(ctx.getNodeStatus("success")).isEqualTo(NodeStatus.SUCCESS);
        verify(ctxStore).saveNodeState(eq("exec-pending-test"), eq("success"),
                eq(NodeStatus.SUCCESS), eq(Map.of("ok", true)));
    }

    private DagEngine engineWithHandlers(NodeHandler... handlers) {
        return engineWithHandlers(mock(ContextPersistenceService.class), handlers);
    }

    private DagEngine engineWithHandlers(ContextPersistenceService ctxStore, NodeHandler... handlers) {
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

    @NodeHandlerType("TEST_SUCCESS")
    static class TestSuccessHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.terminal(Map.of("ok", true)));
        }
    }
}
