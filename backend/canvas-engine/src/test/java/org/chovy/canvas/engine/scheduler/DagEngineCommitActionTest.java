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
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DagEngineCommitActionTest {

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

    private DagEngine engineWithHandlers(NodeHandler... handlers) {
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

    @NodeHandlerType("TEST_FAIL")
    static class TestFailHandler implements NodeHandler {
        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            return Mono.just(NodeResult.fail("boom"));
        }
    }
}
