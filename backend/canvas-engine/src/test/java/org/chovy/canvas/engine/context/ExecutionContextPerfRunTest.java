package org.chovy.canvas.engine.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionContextPerfRunTest {

    @Test
    void storesPerfRunIdOnExecutionContext() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setPerfRunId("perf_20260523_001");

        assertThat(ctx.getPerfRunId()).isEqualTo("perf_20260523_001");
    }
}
