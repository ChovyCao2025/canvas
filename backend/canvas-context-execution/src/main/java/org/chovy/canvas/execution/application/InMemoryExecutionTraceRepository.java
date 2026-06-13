package org.chovy.canvas.execution.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.chovy.canvas.execution.api.trace.ExecutionTraceView;

public class InMemoryExecutionTraceRepository implements ExecutionTraceRepository {

    private final Map<Key, MutableTrace> traces = new ConcurrentHashMap<>();

    @Override
    public void saveStarted(ExecutionTraceRecord trace) {
        traces.put(new Key(trace.tenantId(), trace.executionId()), new MutableTrace(
                trace.tenantId(),
                trace.executionId(),
                trace.canvasId(),
                trace.status(),
                trace.startedAt(),
                trace.finishedAt(),
                trace.failureReason()));
    }

    @Override
    public void appendNode(ExecutionNodeTraceRecord nodeTrace) {
        MutableTrace trace = trace(nodeTrace.tenantId(), nodeTrace.executionId());
        trace.nodeResults.add(new ExecutionTraceView.NodeResultView(
                nodeTrace.nodeId(),
                nodeTrace.nodeType(),
                nodeTrace.status(),
                nodeTrace.error(),
                nodeTrace.outputData()));
    }

    @Override
    public void markFinished(Long tenantId, String executionId, String status, String failureReason, Instant finishedAt) {
        MutableTrace trace = trace(tenantId, executionId);
        trace.status = status == null || status.isBlank() ? "UNKNOWN" : status;
        trace.failureReason = failureReason == null ? "" : failureReason;
        trace.finishedAt = finishedAt == null ? Instant.now() : finishedAt;
    }

    @Override
    public ExecutionTraceView get(Long tenantId, String executionId) {
        MutableTrace trace = trace(tenantId, executionId);
        return new ExecutionTraceView(
                trace.tenantId,
                trace.executionId,
                trace.canvasId,
                trace.status,
                trace.startedAt,
                trace.finishedAt,
                List.copyOf(trace.nodeResults),
                trace.failureReason);
    }

    private MutableTrace trace(Long tenantId, String executionId) {
        MutableTrace trace = traces.get(new Key(tenantId, executionId));
        if (trace == null) {
            throw new IllegalStateException("execution trace not found: tenantId=" + tenantId
                    + ", executionId=" + executionId);
        }
        return trace;
    }

    private record Key(Long tenantId, String executionId) {
    }

    private static final class MutableTrace {
        private final Long tenantId;
        private final String executionId;
        private final Long canvasId;
        private final Instant startedAt;
        private final List<ExecutionTraceView.NodeResultView> nodeResults = new ArrayList<>();
        private String status;
        private Instant finishedAt;
        private String failureReason;

        private MutableTrace(
                Long tenantId,
                String executionId,
                Long canvasId,
                String status,
                Instant startedAt,
                Instant finishedAt,
                String failureReason) {
            this.tenantId = tenantId;
            this.executionId = executionId;
            this.canvasId = canvasId;
            this.status = status;
            this.startedAt = startedAt;
            this.finishedAt = finishedAt;
            this.failureReason = failureReason;
        }
    }
}
