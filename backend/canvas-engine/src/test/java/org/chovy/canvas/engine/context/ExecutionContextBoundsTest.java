package org.chovy.canvas.engine.context;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionContextBoundsTest {

    @Test
    void calculatesSerializedContextSize() {
        ExecutionContext ctx = new ExecutionContext();

        ctx.putNodeOutput("node-a", Map.of("field", "value"));

        int size = ctx.calculateSerializedContextSize();
        assertThat(size).isPositive();
        assertThat(ctx.getApproxSizeBytes()).isEqualTo(size);
    }

    @Test
    void emitsWarningAtThreshold() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setWarnSizeBytes(16);

        ctx.putNodeOutput("node-a", Map.of("payload", "x".repeat(64)));

        assertThat(ctx.isNearSizeLimit()).isTrue();
        assertThat(ctx.isOversized()).isFalse();
    }

    @Test
    void rejectsAboveHardMax() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setMaxSizeBytes(48);

        assertThatThrownBy(() -> ctx.putNodeOutput("node-a", Map.of("payload", "x".repeat(128))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("execution context exceeds max bytes");
        assertThat(ctx.getNodeOutputs()).doesNotContainKey("node-a");
        assertThat(ctx.getContextValue("node-a.payload")).isNull();
    }

    @Test
    void storesNodeScopedFlatKeys() {
        ExecutionContext ctx = new ExecutionContext();

        ctx.putNodeOutput("node-a", Map.of("result", "a"));
        ctx.putNodeOutput("node-b", Map.of("result", "b"));

        assertThat(ctx.getContextValue("node-a.result")).isEqualTo("a");
        assertThat(ctx.getContextValue("node-b.result")).isEqualTo("b");
    }

    @Test
    void readsLegacyNestedContextForCompatibility() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.putNodeOutput("node-a", Map.of("legacyField", "legacy-value"));
        ctx.setContextValue("node-a.legacyField", null);

        assertThat(ctx.getContextValue("node-a.legacyField")).isEqualTo("legacy-value");
        assertThat(ctx.getContextValue("legacyField")).isEqualTo("legacy-value");
    }
}
