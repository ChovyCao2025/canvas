package org.chovy.canvas.execution.adapter.plugin.official.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.domain.DagNode;
import org.chovy.canvas.execution.domain.NodeExecutionContext;
import org.chovy.canvas.execution.domain.NodeExecutionResult;
import org.chovy.canvas.execution.domain.NodeHandlerRegistry;
import org.junit.jupiter.api.Test;

class OfficialMessagePluginTest {

    @Test
    void registersMessageSendHandlerThroughExecutionRegistry() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry(List.of(new OfficialMessageNodeHandler()));

        assertThat(registry.has("message.send")).isTrue();
        assertThat(registry.metadata()).extracting(metadata -> metadata.nodeType())
                .containsExactly("message.send");
    }

    @Test
    void returnsNormalizedMessageSendEnvelope() {
        OfficialMessageNodeHandler handler = new OfficialMessageNodeHandler();
        DagNode node = new DagNode(
                "message-1",
                "message.send",
                "Welcome Message",
                Map.of(
                        "channel", "sms",
                        "template", "welcome_coupon",
                        "recipient", "${payload.phone}"),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext(
                "exec-1",
                node,
                "user-1",
                Map.of("phone", "+15550001111"),
                Map.of("tenantId", "tenant-a")));

        assertThat(result.success()).isTrue();
        assertThat(result.pending()).isFalse();
        assertThat(result.error()).isEmpty();
        assertThat(result.output()).containsEntry("pluginId", "canvas-plugin-message")
                .containsEntry("nodeType", "message.send")
                .containsEntry("channel", "sms")
                .containsEntry("template", "welcome_coupon")
                .containsEntry("recipient", "+15550001111")
                .containsEntry("delivery", "stub")
                .containsEntry("status", "SENT");
        assertThat(result.output().get("payload")).isEqualTo(Map.of("phone", "+15550001111"));
        assertThat(result.output().get("context")).isEqualTo(Map.of("tenantId", "tenant-a"));
    }

    @Test
    void defaultsChannelRecipientAndTrimsTemplateConfig() {
        OfficialMessageNodeHandler handler = new OfficialMessageNodeHandler();
        DagNode node = new DagNode(
                "message-1",
                "message.send",
                "Welcome Message",
                Map.of(
                        "channel", "  ",
                        "template", " welcome_coupon "),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext(
                "exec-1",
                node,
                "user-1",
                Map.of(),
                Map.of()));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("channel", "sms")
                .containsEntry("template", "welcome_coupon")
                .containsEntry("recipient", "user-1");
    }

    @Test
    void preservesLiteralRecipientWhenConfiguredValueDoesNotResolve() {
        OfficialMessageNodeHandler handler = new OfficialMessageNodeHandler();
        DagNode node = new DagNode(
                "message-1",
                "message.send",
                "Welcome Message",
                Map.of(
                        "template", "welcome_coupon",
                        "recipient", "+15550001111"),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext(
                "exec-1",
                node,
                "user-1",
                Map.of(),
                Map.of()));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("recipient", "+15550001111");
    }

    @Test
    void fallsBackWhenConfiguredRecipientReferenceDoesNotResolve() {
        OfficialMessageNodeHandler handler = new OfficialMessageNodeHandler();
        DagNode templateReferenceNode = new DagNode(
                "message-1",
                "message.send",
                "Welcome Message",
                Map.of(
                        "template", "welcome_coupon",
                        "recipient", "${payload.missing}"),
                Map.of());
        DagNode pathReferenceNode = new DagNode(
                "message-2",
                "message.send",
                "Welcome Message",
                Map.of(
                        "template", "welcome_coupon",
                        "recipient", "payload.missing"),
                Map.of());

        NodeExecutionResult templateReferenceResult = handler.execute(new NodeExecutionContext(
                "exec-1",
                templateReferenceNode,
                "user-1",
                Map.of(),
                Map.of()));
        NodeExecutionResult pathReferenceResult = handler.execute(new NodeExecutionContext(
                "exec-1",
                pathReferenceNode,
                "user-1",
                Map.of(),
                Map.of()));

        assertThat(templateReferenceResult.success()).isTrue();
        assertThat(templateReferenceResult.output()).containsEntry("recipient", "user-1");
        assertThat(pathReferenceResult.success()).isTrue();
        assertThat(pathReferenceResult.output()).containsEntry("recipient", "user-1");
    }

    @Test
    void fallsBackToAnonymousWhenRecipientAndUserIdAreMissing() {
        OfficialMessageNodeHandler handler = new OfficialMessageNodeHandler();
        DagNode node = new DagNode(
                "message-1",
                "message.send",
                "Welcome Message",
                Map.of("template", "welcome_coupon"),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("recipient", "anonymous");
    }

    @Test
    void failsWhenMessageTemplateIsMissing() {
        OfficialMessageNodeHandler handler = new OfficialMessageNodeHandler();
        DagNode node = new DagNode(
                "message-1",
                "message.send",
                "Welcome Message",
                Map.of("channel", "sms"),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("message template is required");
    }
}
