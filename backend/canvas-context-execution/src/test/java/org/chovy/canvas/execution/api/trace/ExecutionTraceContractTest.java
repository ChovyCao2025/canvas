package org.chovy.canvas.execution.api.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.CanvasExecutionFacade;
import org.junit.jupiter.api.Test;

class ExecutionTraceContractTest {

    @Test
    void traceViewIsImmutableAndReadableThroughExecutionFacade() {
        List<ExecutionTraceView.NodeResultView> nodeResults = new ArrayList<>();
        nodeResults.add(new ExecutionTraceView.NodeResultView(
                "message",
                "message.send",
                "FAILED",
                "provider timeout",
                Map.of("attempt", 1)));
        ExecutionTraceView trace = new ExecutionTraceView(
                2L,
                "exec-99",
                10L,
                "FAILED",
                Instant.parse("2026-06-10T01:00:00Z"),
                Instant.parse("2026-06-10T01:01:00Z"),
                nodeResults,
                "provider timeout");
        CanvasExecutionFacade facade = new StaticTraceFacade(trace);

        nodeResults.clear();

        assertThat(facade.trace(2L, "exec-99")).isSameAs(trace);
        assertThat(trace.nodeResults()).hasSize(1);
        assertThat(trace.nodeResults().get(0).error()).contains("timeout");
        assertThatThrownBy(() -> trace.nodeResults().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private record StaticTraceFacade(ExecutionTraceView trace) implements CanvasExecutionFacade {
        @Override
        public ExecutionResultView trigger(ExecutionRequestCommand command) {
            return new ExecutionResultView("exec-1", "STARTED");
        }

        @Override
        public ExecutionTraceView trace(Long tenantId, String executionId) {
            return trace;
        }
    }
}
