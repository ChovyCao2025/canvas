package org.chovy.canvas.engine.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    void putNodeOutput_acceptsWhenSerializedContextFitsEvenIfValueToStringIsLarge() {
        ExecutionContext ctx = new ExecutionContext();

        ctx.putNodeOutput("node-A", Map.of("value", new CompactJsonValue()));

        assertThat(ctx.getNodeOutput("node-A", "value")).isInstanceOf(CompactJsonValue.class);
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
    void triggerPayloadDeeplyCopiesInputAndReturnsDeeplyReadOnlySnapshot() {
        ExecutionContext ctx = new ExecutionContext();
        Map<String, Object> profile = new HashMap<>();
        profile.put("tier", "gold");
        List<String> tags = new ArrayList<>(List.of("vip"));
        Map<String, Object> payload = new HashMap<>();
        payload.put("profile", profile);
        payload.put("tags", tags);

        ctx.setTriggerPayload(payload);
        int originalSize = ctx.getApproxSizeBytes();

        profile.put("tier", "platinum");
        tags.add("oversized".repeat(200_000));

        assertThat(ctx.getContextValue("profile")).isEqualTo(Map.of("tier", "gold"));
        assertThat(ctx.getContextValue("tags")).isEqualTo(List.of("vip"));
        assertThat(ctx.getApproxSizeBytes()).isEqualTo(originalSize);

        Map<String, Object> returnedProfile = (Map<String, Object>) ctx.getTriggerPayload().get("profile");
        List<String> returnedTags = (List<String>) ctx.getTriggerPayload().get("tags");
        assertThatThrownBy(() -> returnedProfile.put("tier", "mutated"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> returnedTags.add("mutated"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(ctx.getContextValue("profile")).isEqualTo(Map.of("tier", "gold"));
        assertThat(ctx.getApproxSizeBytes()).isEqualTo(originalSize);
    }

    @Test
    void putTriggerPayloadValuesDeeplyCopiesInputAliases() {
        ExecutionContext ctx = new ExecutionContext();
        List<String> items = new ArrayList<>(List.of("initial"));
        Map<String, Object> values = new HashMap<>();
        values.put("items", items);

        ctx.putTriggerPayloadValues(values);
        int originalSize = ctx.getApproxSizeBytes();

        items.add("oversized".repeat(200_000));

        assertThat(ctx.getContextValue("items")).isEqualTo(List.of("initial"));
        assertThat(ctx.getApproxSizeBytes()).isEqualTo(originalSize);
    }

    @Test
    void triggerPayloadConvertsArraysToImmutableSnapshots() {
        ExecutionContext ctx = new ExecutionContext();
        String[] codes = {"A"};

        ctx.setTriggerPayload(Map.of("codes", codes));
        int originalSize = ctx.getApproxSizeBytes();
        codes[0] = "B";

        assertThat(ctx.getContextValue("codes")).isEqualTo(List.of("A"));
        assertThat(ctx.getApproxSizeBytes()).isEqualTo(originalSize);
        List<String> returnedCodes = (List<String>) ctx.getTriggerPayload().get("codes");
        assertThatThrownBy(() -> returnedCodes.add("C"))
                .isInstanceOf(UnsupportedOperationException.class);
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

    static class CompactJsonValue {
        public String getValue() {
            return "small";
        }

        @Override
        public String toString() {
            return "x".repeat(1_100_000);
        }
    }
}
