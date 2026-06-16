package org.chovy.canvas.execution.adapter.plugin.official.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.domain.DagNode;
import org.chovy.canvas.execution.domain.NodeExecutionContext;
import org.chovy.canvas.execution.domain.NodeExecutionResult;
import org.chovy.canvas.execution.domain.NodeHandler;
import org.chovy.canvas.execution.domain.NodeHandlerRegistry;
import org.junit.jupiter.api.Test;

class OfficialAiPluginTest {

    @Test
    void registersAiGenerateCopyHandlerThroughExecutionRegistry() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry(List.of(new OfficialAiNodeHandler()));

        assertThat(registry.has("ai.generate-copy")).isTrue();
        assertThat(registry.metadata()).extracting(metadata -> metadata.nodeType())
                .containsExactly("ai.generate-copy");
    }

    @Test
    void returnsDeterministicAiGenerateCopyEnvelope() {
        NodeHandler handler = new OfficialAiNodeHandler();
        DagNode node = new DagNode(
                "generate",
                "ai.generate-copy",
                "Generate Copy",
                Map.of("promptKey", "seasonal_offer"),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext(
                "exec-1",
                node,
                "operator-1",
                Map.of("campaign", Map.of("offer", "summer bundle")),
                Map.of("tenantId", "tenant-a")));

        assertThat(result.success()).isTrue();
        assertThat(result.pending()).isFalse();
        assertThat(result.error()).isEmpty();
        assertThat(result.output()).containsEntry("pluginId", "canvas-plugin-ai")
                .containsEntry("nodeType", "ai.generate-copy")
                .containsEntry("promptKey", "seasonal_offer")
                .containsEntry("operator", "operator-1")
                .containsEntry("generation", "stub")
                .containsEntry("status", "GENERATED")
                .containsEntry("generatedCopy", "Generated copy for seasonal_offer");
        assertThat(result.output().get("payload")).isEqualTo(Map.of("campaign", Map.of("offer", "summer bundle")));
        assertThat(result.output().get("context")).isEqualTo(Map.of("tenantId", "tenant-a"));
    }

    @Test
    void trimsPromptKey() {
        NodeHandler handler = new OfficialAiNodeHandler();
        DagNode node = new DagNode(
                "generate",
                "ai.generate-copy",
                "Generate Copy",
                Map.of("promptKey", " seasonal_offer "),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext(
                "exec-1",
                node,
                "operator-1",
                Map.of(),
                Map.of()));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("promptKey", "seasonal_offer")
                .containsEntry("generatedCopy", "Generated copy for seasonal_offer");
    }

    @Test
    void defaultsOperatorToAnonymousWhenUserIdIsMissing() {
        NodeHandler handler = new OfficialAiNodeHandler();
        DagNode node = new DagNode(
                "generate",
                "ai.generate-copy",
                "Generate Copy",
                Map.of("promptKey", "seasonal_offer"),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("operator", "anonymous");
    }

    @Test
    void failsWhenPromptKeyIsMissing() {
        NodeHandler handler = new OfficialAiNodeHandler();
        DagNode node = new DagNode(
                "generate",
                "ai.generate-copy",
                "Generate Copy",
                Map.of(),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("AI prompt key is required");
    }

    @Test
    void failsWhenPromptKeyIsBlank() {
        NodeHandler handler = new OfficialAiNodeHandler();
        DagNode node = new DagNode(
                "generate",
                "ai.generate-copy",
                "Generate Copy",
                Map.of("promptKey", "  "),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("AI prompt key is required");
    }
}
