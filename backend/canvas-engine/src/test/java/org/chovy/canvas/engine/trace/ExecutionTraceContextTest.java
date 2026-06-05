package org.chovy.canvas.engine.trace;

import org.chovy.canvas.config.CorrelationIdWebFilter;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionTraceContextTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void opensExecutionAndNodeFieldsAndRestoresPreviousMdc() {
        MDC.put(CorrelationIdWebFilter.MDC_KEY, "request-trace");
        MDC.put("unchanged", "keep");
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(42L);

        ExecutionTraceContext traceContext = ExecutionTraceContext.from(ctx, "node-a");

        try (ExecutionTraceContext.Scope ignored = traceContext.open()) {
            assertThat(MDC.get(ExecutionTraceContext.EXECUTION_ID_KEY)).isEqualTo("exec-1");
            assertThat(MDC.get(ExecutionTraceContext.CANVAS_ID_KEY)).isEqualTo("42");
            assertThat(MDC.get(ExecutionTraceContext.NODE_ID_KEY)).isEqualTo("node-a");
            assertThat(MDC.get(ExecutionTraceContext.CORRELATION_ID_KEY)).isEqualTo("request-trace");
            assertThat(MDC.get("unchanged")).isEqualTo("keep");
        }

        assertThat(MDC.get(ExecutionTraceContext.EXECUTION_ID_KEY)).isNull();
        assertThat(MDC.get(ExecutionTraceContext.CANVAS_ID_KEY)).isNull();
        assertThat(MDC.get(ExecutionTraceContext.NODE_ID_KEY)).isNull();
        assertThat(MDC.get(CorrelationIdWebFilter.MDC_KEY)).isEqualTo("request-trace");
        assertThat(MDC.get("unchanged")).isEqualTo("keep");
    }

    @Test
    void exposesTraceFieldsAsMdcMap() {
        ExecutionTraceContext traceContext = new ExecutionTraceContext(
                "exec-1", 7L, "node-b", "corr-1");

        assertThat(traceContext.asMdcMap()).containsAllEntriesOf(Map.of(
                ExecutionTraceContext.EXECUTION_ID_KEY, "exec-1",
                ExecutionTraceContext.CANVAS_ID_KEY, "7",
                ExecutionTraceContext.NODE_ID_KEY, "node-b",
                ExecutionTraceContext.CORRELATION_ID_KEY, "corr-1"
        ));
    }
}
