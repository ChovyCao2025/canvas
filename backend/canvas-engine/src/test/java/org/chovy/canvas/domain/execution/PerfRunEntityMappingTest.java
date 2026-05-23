package org.chovy.canvas.domain.execution;

import org.chovy.canvas.domain.meta.EventLog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PerfRunEntityMappingTest {

    @Test
    void eventLogExposesPerfRunId() {
        EventLog log = new EventLog();
        log.setPerfRunId("perf_20260523_001");

        assertThat(log.getPerfRunId()).isEqualTo("perf_20260523_001");
    }

    @Test
    void executionExposesPerfRunId() {
        CanvasExecution execution = new CanvasExecution();
        execution.setPerfRunId("perf_20260523_001");

        assertThat(execution.getPerfRunId()).isEqualTo("perf_20260523_001");
    }

    @Test
    void executionRequestExposesPerfRunId() {
        CanvasExecutionRequest request = new CanvasExecutionRequest();
        request.setPerfRunId("perf_20260523_001");

        assertThat(request.getPerfRunId()).isEqualTo("perf_20260523_001");
    }

    @Test
    void dlqExposesPerfRunId() {
        CanvasExecutionDlq dlq = CanvasExecutionDlq.builder()
                .perfRunId("perf_20260523_001")
                .build();

        assertThat(dlq.getPerfRunId()).isEqualTo("perf_20260523_001");
    }
}
