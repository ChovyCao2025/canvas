package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasEdgeDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasNodeDefinition;
import org.chovy.canvas.execution.api.CanvasExecutionFacade.ExecutionRequestCommand;
import org.chovy.canvas.execution.api.CanvasExecutionFacade.ExecutionResultView;
import org.chovy.canvas.execution.api.trace.ExecutionTraceView;
import org.chovy.canvas.execution.domain.AggregateNodeHandler;
import org.chovy.canvas.execution.domain.DagRuntimeService;
import org.chovy.canvas.execution.domain.EndNodeHandler;
import org.chovy.canvas.execution.domain.NodeExecutionContext;
import org.chovy.canvas.execution.domain.NodeExecutionResult;
import org.chovy.canvas.execution.domain.NodeHandler;
import org.chovy.canvas.execution.domain.NodeHandlerRegistry;
import org.chovy.canvas.execution.domain.NodeHandlerType;
import org.chovy.canvas.execution.domain.StartNodeHandler;
import org.junit.jupiter.api.Test;

class CanvasExecutionApplicationServiceTest {

    @Test
    void triggerExecutesPublishedDefinitionAndExposesTraceThroughFacade() {
        ExecutionDefinitionRepository repository = new InMemoryExecutionDefinitionRepository();
        ExecutionTraceService traceService = new ExecutionTraceService(new InMemoryExecutionTraceRepository());
        CanvasExecutionApplicationService service = new CanvasExecutionApplicationService(
                new DagRuntimeService(),
                new NodeHandlerRegistry(List.of(new StartNodeHandler(), new CapturePayloadHandler(), new EndNodeHandler())),
                traceService,
                repository);
        repository.save(definition());

        ExecutionResultView result = service.trigger(new ExecutionRequestCommand(
                9L,
                30L,
                31L,
                "MANUAL",
                "user-7",
                Map.of("couponCode", "A10"),
                false));
        ExecutionTraceView trace = service.trace(9L, result.executionId());

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(trace.status()).isEqualTo("SUCCESS");
        assertThat(trace.canvasId()).isEqualTo(30L);
        assertThat(trace.nodeResults()).extracting(ExecutionTraceView.NodeResultView::nodeId)
                .containsExactly("start", "capture", "end");
        assertThat(trace.nodeResults().get(1).outputData())
                .containsEntry("capturedUser", "user-7")
                .containsEntry("couponCode", "A10");
    }

    @Test
    void triggerMakesNodeStatusesAndOutputsAvailableToAggregateHandlers() {
        ExecutionDefinitionRepository repository = new InMemoryExecutionDefinitionRepository();
        ExecutionTraceService traceService = new ExecutionTraceService(new InMemoryExecutionTraceRepository());
        CanvasExecutionApplicationService service = new CanvasExecutionApplicationService(
                new DagRuntimeService((configJson, nodeId) -> "aggregate".equals(nodeId)
                        ? Map.of(
                                "upstreamIds", List.of("email", "sms"),
                                "evaluateMode", "count",
                                "minCount", 2,
                                "successNodeId", "end-success",
                                "failNodeId", "end-fail")
                        : Map.of()),
                new NodeHandlerRegistry(List.of(
                        new StartNodeHandler(),
                        new ChannelSuccessHandler(),
                        new AggregateNodeHandler(),
                        new EndNodeHandler())),
                traceService,
                repository);
        repository.save(aggregateDefinition());

        ExecutionResultView result = service.trigger(new ExecutionRequestCommand(
                9L,
                30L,
                31L,
                "MANUAL",
                "user-7",
                Map.of(),
                false));
        ExecutionTraceView trace = service.trace(9L, result.executionId());

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(trace.nodeResults()).extracting(ExecutionTraceView.NodeResultView::nodeId)
                .contains("aggregate", "end-success")
                .doesNotContain("end-fail");
        assertThat(trace.nodeResults().stream()
                .filter(node -> "aggregate".equals(node.nodeId()))
                .findFirst()
                .orElseThrow()
                .outputData())
                .containsEntry("successCount", 2L)
                .containsEntry("passed", true);
    }

    @Test
    void aggregateWaitsForAllConfiguredUpstreamBranchesBeforeExecuting() {
        ExecutionDefinitionRepository repository = new InMemoryExecutionDefinitionRepository();
        ExecutionTraceService traceService = new ExecutionTraceService(new InMemoryExecutionTraceRepository());
        CanvasExecutionApplicationService service = new CanvasExecutionApplicationService(
                new DagRuntimeService((configJson, nodeId) -> "aggregate".equals(nodeId)
                        ? Map.of(
                                "upstreamIds", List.of("fast", "slow2"),
                                "evaluateMode", "count",
                                "minCount", 2,
                                "successNodeId", "end-success",
                                "failNodeId", "end-fail")
                        : Map.of()),
                new NodeHandlerRegistry(List.of(
                        new StartNodeHandler(),
                        new ChannelSuccessHandler(),
                        new AggregateNodeHandler(),
                        new EndNodeHandler())),
                traceService,
                repository);
        repository.save(unbalancedAggregateDefinition());

        ExecutionResultView result = service.trigger(new ExecutionRequestCommand(
                9L,
                30L,
                31L,
                "MANUAL",
                "user-7",
                Map.of(),
                false));
        ExecutionTraceView trace = service.trace(9L, result.executionId());

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(trace.nodeResults()).extracting(ExecutionTraceView.NodeResultView::nodeId)
                .containsSubsequence("slow2", "aggregate", "end-success")
                .doesNotContain("end-fail");
    }

    private static PublishedCanvasDefinition definition() {
        return new PublishedCanvasDefinition(
                9L,
                30L,
                31L,
                2,
                "{\"nodes\":[]}",
                Instant.parse("2026-06-10T04:20:00Z"),
                Map.of("triggerType", "MANUAL"),
                List.of(
                        node("start", "START"),
                        node("capture", "CAPTURE_PAYLOAD"),
                        node("end", "END")),
                List.of(
                        edge("start", "capture"),
                        edge("capture", "end")));
    }

    private static PublishedCanvasDefinition aggregateDefinition() {
        return new PublishedCanvasDefinition(
                9L,
                30L,
                31L,
                2,
                "{\"nodes\":[]}",
                Instant.parse("2026-06-10T04:20:00Z"),
                Map.of("triggerType", "MANUAL"),
                List.of(
                        node("start", "START"),
                        node("email", "CHANNEL_SUCCESS"),
                        node("sms", "CHANNEL_SUCCESS"),
                        node("aggregate", "AGGREGATE"),
                        node("end-success", "END"),
                        node("end-fail", "END")),
                List.of(
                        edge("start", "email"),
                        edge("start", "sms"),
                        edge("email", "aggregate"),
                        edge("sms", "aggregate"),
                        edge("aggregate", "end-success"),
                        edge("aggregate", "end-fail")));
    }

    private static PublishedCanvasDefinition unbalancedAggregateDefinition() {
        return new PublishedCanvasDefinition(
                9L,
                30L,
                31L,
                2,
                "{\"nodes\":[]}",
                Instant.parse("2026-06-10T04:20:00Z"),
                Map.of("triggerType", "MANUAL"),
                List.of(
                        node("start", "START"),
                        node("fast", "CHANNEL_SUCCESS"),
                        node("slow", "CHANNEL_SUCCESS"),
                        node("slow2", "CHANNEL_SUCCESS"),
                        node("aggregate", "AGGREGATE"),
                        node("end-success", "END"),
                        node("end-fail", "END")),
                List.of(
                        edge("start", "fast"),
                        edge("start", "slow"),
                        edge("fast", "aggregate"),
                        edge("slow", "slow2"),
                        edge("slow2", "aggregate"),
                        edge("aggregate", "end-success"),
                        edge("aggregate", "end-fail")));
    }

    private static PublishedCanvasNodeDefinition node(String nodeId, String nodeType) {
        return new PublishedCanvasNodeDefinition(nodeId, nodeType, nodeType, "{}", Map.of(), Map.of());
    }

    private static PublishedCanvasEdgeDefinition edge(String source, String target) {
        return new PublishedCanvasEdgeDefinition(source + "-" + target, source, target, "{}", Map.of());
    }

    @NodeHandlerType("CAPTURE_PAYLOAD")
    private static final class CapturePayloadHandler implements NodeHandler {
        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            return NodeExecutionResult.success(Map.of(
                    "capturedUser", context.userId(),
                    "couponCode", context.payload().get("couponCode")));
        }
    }

    @NodeHandlerType("CHANNEL_SUCCESS")
    private static final class ChannelSuccessHandler implements NodeHandler {
        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            return NodeExecutionResult.success(Map.of(
                    "status", "SUCCESS",
                    "channel", context.node().nodeId()));
        }
    }
}
