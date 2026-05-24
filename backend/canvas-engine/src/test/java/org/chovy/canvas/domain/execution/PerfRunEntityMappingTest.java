package org.chovy.canvas.domain.execution;

import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;

class PerfRunEntityMappingTest {

    @Test
    void eventLogExposesPerfRunId() {
        EventLogDO log = new EventLogDO();
        log.setPerfRunId("perf_20260523_001");

        assertThat(log.getPerfRunId()).isEqualTo("perf_20260523_001");
    }

    @Test
    void executionExposesPerfRunId() {
        CanvasExecutionDO execution = new CanvasExecutionDO();
        execution.setPerfRunId("perf_20260523_001");

        assertThat(execution.getPerfRunId()).isEqualTo("perf_20260523_001");
    }

    @Test
    void executionRequestExposesPerfRunId() {
        CanvasExecutionRequestDO request = new CanvasExecutionRequestDO();
        request.setPerfRunId("perf_20260523_001");

        assertThat(request.getPerfRunId()).isEqualTo("perf_20260523_001");
    }

    @Test
    void dlqExposesPerfRunId() {
        CanvasExecutionDlqDO dlq = CanvasExecutionDlqDO.builder()
                .perfRunId("perf_20260523_001")
                .build();

        assertThat(dlq.getPerfRunId()).isEqualTo("perf_20260523_001");
    }
}
