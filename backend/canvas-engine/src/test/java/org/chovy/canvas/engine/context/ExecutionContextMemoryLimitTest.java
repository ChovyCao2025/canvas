package org.chovy.canvas.engine.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionContextMemoryLimitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    void putNodeOutput_rejectsUtf8SerializedContextOverLimit() {
        ExecutionContext ctx = new ExecutionContext();
        String largeValue = "汉".repeat(400_000);

        assertThatThrownBy(() -> ctx.putNodeOutput("oversized", Map.of("data", largeValue)))
                .isInstanceOf(ContextOverflowException.class)
                .hasMessageContaining("1MB");
        assertThat(ctx.getNodeOutputs()).isEmpty();
        assertThat(ctx.getApproxSizeBytes()).isLessThan(1_024 * 1_024);
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

    @Test
    void serializedContextDoesNotDuplicateDerivedFlatContext() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-serialized-size");

        ctx.putNodeOutput("node-A", Map.of("data", "x".repeat(600_000)));

        byte[] json = objectMapper.writeValueAsBytes(ctx);
        assertThat(ctx.isOversized()).isFalse();
        assertThat(json.length).isLessThan(1_024 * 1_024);
        assertThat(new String(json, java.nio.charset.StandardCharsets.UTF_8))
                .doesNotContain("flatContext");
    }

    @Test
    void triggerPayloadSetterEnforcesSerializedContextLimitAndUpdatesAccounting() {
        ExecutionContext ctx = new ExecutionContext();

        assertThatThrownBy(() -> ctx.setTriggerPayload(Map.of("data", "汉".repeat(400_000))))
                .isInstanceOf(ContextOverflowException.class)
                .hasMessageContaining("1MB");

        assertThat(ctx.getTriggerPayload()).isEmpty();
        assertThat(ctx.getApproxSizeBytes()).isLessThan(1_024 * 1_024);

        ctx.setTriggerPayload(Map.of("data", "ok"));

        assertThat(ctx.getTriggerPayload()).containsEntry("data", "ok");
        assertThat(ctx.getApproxSizeBytes()).isGreaterThan(0);
    }

    @Test
    void triggerPayloadGetterIsReadOnlySoSizeAccountingCannotBeBypassed() {
        ExecutionContext ctx = new ExecutionContext();

        assertThatThrownBy(() -> ctx.getTriggerPayload().put("data", "x"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(ctx.getApproxSizeBytes()).isLessThan(1_024 * 1_024);
    }

    @Test
    void rebuildDerivedStateRestoresSizeAccountingAfterDeserialize() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-rebuild");
        ctx.putNodeOutput("node-A", Map.of("data", "x".repeat(700_000)));

        ExecutionContext restored = objectMapper.readValue(
                objectMapper.writeValueAsBytes(ctx),
                ExecutionContext.class);
        restored.rebuildDerivedState();

        assertThat(restored.getNodeOutput("node-A", "data")).isEqualTo("x".repeat(700_000));
        assertThat(restored.getApproxSizeBytes()).isGreaterThan(700_000);

        restored.putNodeOutput("node-B", Map.of("data", "y".repeat(700_000)));

        assertThat(restored.isOversized()).isFalse();
        assertThat(restored.getNodeOutput("node-A", "data")).isNull();
        assertThat(restored.getNodeOutput("node-B", "data")).isEqualTo("y".repeat(700_000));
    }
}
