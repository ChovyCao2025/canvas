package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.trace.ExecutionTraceView;
import org.junit.jupiter.api.Test;

class ExecutionTracePersistenceServiceTest {

    @Test
    void traceLifecyclePersistsThroughRepositoryContract() {
        RecordingExecutionTraceRepository repository = new RecordingExecutionTraceRepository();
        ExecutionTraceService service = new ExecutionTraceService(repository);

        service.start(8L, "exec-persist-1", 19L, 21L);
        service.recordNode(8L, "exec-persist-1", "start", "START", "SUCCESS", "", Map.of("started", true));
        service.finish(8L, "exec-persist-1", "SUCCESS", "");

        ExecutionTraceView trace = service.trace(8L, "exec-persist-1");

        assertThat(repository.calls).containsExactly("start", "node:start", "finish:SUCCESS");
        assertThat(trace.status()).isEqualTo("SUCCESS");
        assertThat(trace.nodeResults()).hasSize(1);
        assertThat(trace.nodeResults().get(0).outputData()).containsEntry("started", true);
    }

    @Test
    void resumeAppendsToExistingTraceWithoutStartingAgain() {
        RecordingExecutionTraceRepository repository = new RecordingExecutionTraceRepository();
        ExecutionTraceService service = new ExecutionTraceService(repository);

        service.start(8L, "exec-resume-1", 19L, 21L);
        service.recordResume(8L, 19L, 21L, "exec-resume-1", "form", "RESUMED", Map.of("answer", "yes"));

        ExecutionTraceView trace = service.trace(8L, "exec-resume-1");

        assertThat(repository.calls).containsExactly("start", "node:form", "finish:RESUMED");
        assertThat(trace.nodeResults()).hasSize(1);
        assertThat(trace.nodeResults().get(0).outputData()).containsEntry("answer", "yes");
    }

    private static final class RecordingExecutionTraceRepository implements ExecutionTraceRepository {
        private final List<String> calls = new ArrayList<>();
        private final InMemoryExecutionTraceRepository delegate = new InMemoryExecutionTraceRepository();

        @Override
        public void saveStarted(ExecutionTraceRecord trace) {
            calls.add("start");
            assertThat(trace.versionId()).isEqualTo(21L);
            delegate.saveStarted(trace);
        }

        @Override
        public void appendNode(ExecutionNodeTraceRecord nodeTrace) {
            calls.add("node:" + nodeTrace.nodeId());
            delegate.appendNode(nodeTrace);
        }

        @Override
        public void markFinished(Long tenantId, String executionId, String status, String failureReason, Instant finishedAt) {
            calls.add("finish:" + status);
            delegate.markFinished(tenantId, executionId, status, failureReason, finishedAt);
        }

        @Override
        public ExecutionTraceView get(Long tenantId, String executionId) {
            return delegate.get(tenantId, executionId);
        }
    }
}
