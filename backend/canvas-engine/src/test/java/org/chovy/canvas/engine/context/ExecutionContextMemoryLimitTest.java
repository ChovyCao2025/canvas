package org.chovy.canvas.engine.context;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionContextMemoryLimitTest {

    @Test
    void putNodeOutput_rejectsWriteWhenContextCannotFitUnderLimit() {
        ExecutionContext ctx = new ExecutionContext();
        String largeValue = "x".repeat(1_100_000);

        assertThatThrownBy(() -> ctx.putNodeOutput("oversized", Map.of("data", largeValue)))
                .isInstanceOf(ContextOverflowException.class)
                .hasMessageContaining("1MB");
        assertThat(ctx.getFlatContext()).isEmpty();
        assertThat(ctx.getNodeOutputs()).isEmpty();
    }

    @Test
    void putNodeOutput_evictsOldestOutputsBeforeAcceptingNewOutput() {
        ExecutionContext ctx = new ExecutionContext();
        String value = "x".repeat(350_000);

        ctx.putNodeOutput("node-1", Map.of("data", value));
        ctx.putNodeOutput("node-2", Map.of("data", value));
        ctx.putNodeOutput("node-3", Map.of("data", value));

        assertThat(ctx.isOversized()).isFalse();
        assertThat(ctx.getNodeOutput("node-1", "data")).isNull();
        assertThat(ctx.getNodeOutput("node-2", "data")).isNotNull();
        assertThat(ctx.getNodeOutput("node-3", "data")).isNotNull();
    }

    @Test
    void approxSizeBytesAccountsForNestedOutputSerializationShape() {
        ExecutionContext ctx = new ExecutionContext();

        ctx.putNodeOutput("node-A", Map.of(
                "profile", Map.of("tier", "gold", "score", 99),
                "tags", java.util.List.of("vip", "active")));

        assertThat(ctx.getApproxSizeBytes()).isGreaterThan(40);
        assertThat(ctx.isOversized()).isFalse();
    }
}
