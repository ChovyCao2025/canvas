package org.chovy.canvas.engine.context;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionContextNamespaceTest {

    @Test
    void putNodeOutput_namespacesFlatContextKeysToPreventCollisions() {
        ExecutionContext ctx = new ExecutionContext();

        ctx.putNodeOutput("node-A", Map.of("result", "from-A"));
        ctx.putNodeOutput("node-B", Map.of("result", "from-B"));

        assertThat(ctx.getNodeOutput("node-A", "result")).isEqualTo("from-A");
        assertThat(ctx.getNodeOutput("node-B", "result")).isEqualTo("from-B");
        assertThat(ctx.getFlatContext()).containsEntry("node-A.result", "from-A");
        assertThat(ctx.getFlatContext()).containsEntry("node-B.result", "from-B");
        assertThat(ctx.getFlatContext()).doesNotContainKey("result");
    }

    @Test
    void getContextValueSupportsNamespacedExactLookupAndBareLatestCompatibility() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setTriggerPayload(Map.of("status", "from-trigger"));

        ctx.putNodeOutput("node-A", Map.of("status", "pending"));
        ctx.putNodeOutput("node-B", Map.of("status", "completed"));

        assertThat(ctx.getContextValue("node-A.status")).isEqualTo("pending");
        assertThat(ctx.getContextValue("node-B.status")).isEqualTo("completed");
        assertThat(ctx.getContextValue("status")).isEqualTo("completed");
    }

    @Test
    void getContextValueKeepsCompatibilityForBareBusinessKeysContainingDots() {
        ExecutionContext ctx = new ExecutionContext();

        ctx.putNodeOutput("api", Map.of("user.level", "VIP"));

        assertThat(ctx.getContextValue("api.user.level")).isEqualTo("VIP");
        assertThat(ctx.getContextValue("user.level")).isEqualTo("VIP");
    }

    @Test
    void overwritingSameNodeOutputReplacesOldFlatKeysAndSizeEstimate() {
        ExecutionContext ctx = new ExecutionContext();

        ctx.putNodeOutput("node-A", Map.of("old", "value", "count", "1"));
        int firstSize = ctx.getApproxSizeBytes();

        ctx.putNodeOutput("node-A", Map.of("count", "2"));

        assertThat(ctx.getNodeOutput("node-A", "old")).isNull();
        assertThat(ctx.getNodeOutput("node-A", "count")).isEqualTo("2");
        assertThat(ctx.getFlatContext()).doesNotContainKey("node-A.old");
        assertThat(ctx.getApproxSizeBytes()).isLessThan(firstSize);
    }

    @Test
    void getNodeOutputsReturnsReadOnlyInnerOutputMaps() {
        ExecutionContext ctx = new ExecutionContext();

        ctx.putNodeOutput("node-A", Map.of("result", "original"));

        assertThatThrownBy(() -> ctx.getNodeOutputs().get("node-A").put("result", "mutated"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(ctx.getNodeOutput("node-A", "result")).isEqualTo("original");
        assertThat(ctx.getContextValue("result")).isEqualTo("original");
    }

    @Test
    void getNodeOutputsReturnsDeeplyReadOnlyOutputSnapshots() {
        ExecutionContext ctx = new ExecutionContext();

        ctx.putNodeOutput("node-A", Map.of(
                "profile", Map.of("tier", "gold"),
                "tags", List.of("vip")));

        Map<String, Object> profile = (Map<String, Object>) ctx.getNodeOutputs()
                .get("node-A")
                .get("profile");
        List<String> tags = (List<String>) ctx.getNodeOutputs()
                .get("node-A")
                .get("tags");

        assertThatThrownBy(() -> profile.put("tier", "mutated"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> tags.add("mutated"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(ctx.getNodeOutput("node-A", "profile")).isEqualTo(Map.of("tier", "gold"));
        assertThat(ctx.getContextValue("profile")).isEqualTo(Map.of("tier", "gold"));
    }

    @Test
    void putNodeOutputDeeplyCopiesInputAliases() {
        ExecutionContext ctx = new ExecutionContext();
        Map<String, Object> profile = new HashMap<>();
        profile.put("tier", "gold");
        List<String> tags = new ArrayList<>(List.of("vip"));
        Map<String, Object> output = new HashMap<>();
        output.put("profile", profile);
        output.put("tags", tags);

        ctx.putNodeOutput("node-A", output);
        int originalSize = ctx.getApproxSizeBytes();

        profile.put("tier", "platinum");
        tags.add("oversized".repeat(200_000));

        assertThat(ctx.getNodeOutput("node-A", "profile")).isEqualTo(Map.of("tier", "gold"));
        assertThat(ctx.getContextValue("tags")).isEqualTo(List.of("vip"));
        assertThat(ctx.getApproxSizeBytes()).isEqualTo(originalSize);
    }

    @Test
    void putNodeOutputConvertsArraysToImmutableSnapshots() {
        ExecutionContext ctx = new ExecutionContext();
        String[] codes = {"A"};

        ctx.putNodeOutput("node-A", Map.of("codes", codes));
        int originalSize = ctx.getApproxSizeBytes();
        codes[0] = "B";

        assertThat(ctx.getNodeOutput("node-A", "codes")).isEqualTo(List.of("A"));
        assertThat(ctx.getApproxSizeBytes()).isEqualTo(originalSize);
        List<String> returnedCodes = (List<String>) ctx.getNodeOutputs()
                .get("node-A")
                .get("codes");
        assertThatThrownBy(() -> returnedCodes.add("C"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
