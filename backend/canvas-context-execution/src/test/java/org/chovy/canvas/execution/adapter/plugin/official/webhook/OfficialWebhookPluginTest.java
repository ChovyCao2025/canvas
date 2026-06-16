package org.chovy.canvas.execution.adapter.plugin.official.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.domain.DagNode;
import org.chovy.canvas.execution.domain.NodeExecutionContext;
import org.chovy.canvas.execution.domain.NodeExecutionResult;
import org.chovy.canvas.execution.domain.NodeHandlerRegistry;
import org.junit.jupiter.api.Test;

/**
 * 定义 OfficialWebhookPluginTest 的执行上下文数据结构或业务契约。
 */
class OfficialWebhookPluginTest {

    /**
     * 执行 registersWebhookHandlerThroughExecutionRegistry 对应的业务处理。
     */
    @Test
    void registersWebhookHandlerThroughExecutionRegistry() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry(List.of(new OfficialWebhookNodeHandler()));

        assertThat(registry.has("webhook")).isTrue();
        assertThat(registry.metadata()).extracting(metadata -> metadata.nodeType())
                .containsExactly("webhook");
    }

    /**
     * 执行 returnsNormalizedWebhookTriggerEnvelope 对应的业务处理。
     */
    @Test
    void returnsNormalizedWebhookTriggerEnvelope() {
        OfficialWebhookNodeHandler handler = new OfficialWebhookNodeHandler();
        DagNode node = new DagNode(
                "webhook-1",
                "webhook",
                "User Registered",
                Map.of(
                        "event", "user.registered",
                        "source", "public-api"),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext(
                "exec-1",
                node,
                "user-1",
                Map.of("userId", "user-42"),
                Map.of("tenantId", "tenant-a")));

        assertThat(result.success()).isTrue();
        assertThat(result.pending()).isFalse();
        assertThat(result.error()).isEmpty();
        assertThat(result.output()).containsEntry("pluginId", "canvas-plugin-webhook")
                .containsEntry("nodeType", "webhook")
                .containsEntry("event", "user.registered")
                .containsEntry("source", "public-api")
                .containsEntry("received", true);
        assertThat(result.output().get("payload")).isEqualTo(Map.of("userId", "user-42"));
        assertThat(result.output().get("context")).isEqualTo(Map.of("tenantId", "tenant-a"));
    }

    /**
     * 执行 defaultsSourceAndTrimsEventConfig 对应的业务处理。
     */
    @Test
    void defaultsSourceAndTrimsEventConfig() {
        OfficialWebhookNodeHandler handler = new OfficialWebhookNodeHandler();
        DagNode node = new DagNode(
                "webhook-1",
                "webhook",
                "User Registered",
                Map.of(
                        "event", " user.registered ",
                        "source", "  "),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("event", "user.registered")
                .containsEntry("source", "webhook");
    }

    /**
     * 执行 failsWhenWebhookEventIsMissing 对应的业务处理。
     */
    @Test
    void failsWhenWebhookEventIsMissing() {
        OfficialWebhookNodeHandler handler = new OfficialWebhookNodeHandler();
        DagNode node = new DagNode(
                "webhook-1",
                "webhook",
                "User Registered",
                Map.of("source", "public-api"),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("webhook event is required");
    }
}
