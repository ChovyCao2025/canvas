package org.chovy.canvas.execution.api.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TraceExplanationFacadeTest {

    @Test
    void explainsFailedTraceThroughExecutionTraceReaderWithoutPersistenceAccess() {
        RecordingTraceReader reader = new RecordingTraceReader(new ExecutionTraceView(
                7L,
                "exec-42",
                99L,
                "FAILED",
                Instant.parse("2026-06-11T07:59:00Z"),
                Instant.parse("2026-06-11T08:00:00Z"),
                List.of(
                        node("segment", "condition", "SUCCESS", "", Map.of("matched", true)),
                        node("message", "message", "FAILED", "provider timeout after 3000ms", Map.of("provider", "mock-sms"))),
                "message provider timeout"));
        TraceExplanationFacade facade = new TraceExplanationFacade(reader);

        TraceExplanationFacade.TraceExplanation explanation = facade.explain(
                new TraceExplanationFacade.TraceExplanationRequest(7L, "exec-42"));

        assertThat(reader.requests)
                .containsExactly(new TraceExplanationFacade.TraceLookupRequest(7L, "exec-42"));
        assertThat(explanation.executionId()).isEqualTo("exec-42");
        assertThat(explanation.status()).isEqualTo("FAILED");
        assertThat(explanation.failedNodeId()).isEqualTo("message");
        assertThat(explanation.errorType()).isEqualTo("TIMEOUT");
        assertThat(explanation.summary()).contains("message", "provider timeout");
        assertThat(explanation.recommendedActions())
                .contains("Check provider latency, retry policy, and timeout configuration.");
    }

    @Test
    void explainsUnknownFailureWhenTraceHasNoFailedNode() {
        TraceExplanationFacade facade = new TraceExplanationFacade(command -> new ExecutionTraceView(
                7L,
                "exec-77",
                99L,
                "FAILED",
                null,
                null,
                List.of(node("message", "message", "SUCCESS", "", Map.of())),
                "upstream validation failed"));

        TraceExplanationFacade.TraceExplanation explanation = facade.explain(
                new TraceExplanationFacade.TraceExplanationRequest(7L, "exec-77"));

        assertThat(explanation.failedNodeId()).isEmpty();
        assertThat(explanation.errorType()).isEqualTo("VALIDATION");
        assertThat(explanation.recommendedActions())
                .contains("Inspect the failed node configuration and input payload.");
    }

    private static ExecutionTraceView.NodeResultView node(
            String nodeId,
            String nodeType,
            String status,
            String error,
            Map<String, Object> outputData) {
        return new ExecutionTraceView.NodeResultView(nodeId, nodeType, status, error, outputData);
    }

    private static final class RecordingTraceReader implements TraceExplanationFacade.TraceReader {
        private final ExecutionTraceView trace;
        private final List<TraceExplanationFacade.TraceLookupRequest> requests = new ArrayList<>();

        private RecordingTraceReader(ExecutionTraceView trace) {
            this.trace = trace;
        }

        @Override
        public ExecutionTraceView trace(TraceExplanationFacade.TraceLookupRequest request) {
            this.requests.add(request);
            return trace;
        }
    }
}
