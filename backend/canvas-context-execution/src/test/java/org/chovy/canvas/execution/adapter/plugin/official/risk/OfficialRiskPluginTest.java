package org.chovy.canvas.execution.adapter.plugin.official.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.domain.DagNode;
import org.chovy.canvas.execution.domain.NodeExecutionContext;
import org.chovy.canvas.execution.domain.NodeExecutionResult;
import org.chovy.canvas.execution.domain.NodeHandler;
import org.chovy.canvas.execution.domain.NodeHandlerRegistry;
import org.junit.jupiter.api.Test;

class OfficialRiskPluginTest {

    @Test
    void registersRiskCheckHandlerThroughExecutionRegistry() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry(List.of(new OfficialRiskNodeHandler()));

        assertThat(registry.has("risk.check")).isTrue();
        assertThat(registry.metadata()).extracting(metadata -> metadata.nodeType())
                .containsExactly("risk.check");
    }

    @Test
    void returnsAllowedRiskCheckEnvelope() {
        NodeHandler handler = new OfficialRiskNodeHandler();
        DagNode node = new DagNode(
                "risk-1",
                "risk.check",
                "Winback Frequency Check",
                Map.of("policy", "WINBACK_DAILY_CAP"),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext(
                "exec-1",
                node,
                "user-1",
                Map.of("policy", Map.of("dailyTouches", 0)),
                Map.of("tenantId", "tenant-a")));

        assertThat(result.success()).isTrue();
        assertThat(result.pending()).isFalse();
        assertThat(result.error()).isEmpty();
        assertThat(result.output()).containsEntry("pluginId", "canvas-plugin-risk")
                .containsEntry("nodeType", "risk.check")
                .containsEntry("policy", "WINBACK_DAILY_CAP")
                .containsEntry("subject", "user-1")
                .containsEntry("check", "stub")
                .containsEntry("allowed", true)
                .containsEntry("decision", "allowed")
                .containsEntry("status", "MATCHED");
        assertThat(result.output().get("payload")).isEqualTo(Map.of("policy", Map.of("dailyTouches", 0)));
        assertThat(result.output().get("context")).isEqualTo(Map.of("tenantId", "tenant-a"));
    }

    @Test
    void returnsBlockedRiskCheckEnvelopeWhenPolicyRequiresComplianceBlock() {
        NodeHandler handler = new OfficialRiskNodeHandler();
        DagNode node = new DagNode(
                "risk-1",
                "risk.check",
                "Outreach Compliance Check",
                Map.of("policy", "OUTREACH_COMPLIANCE"),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext(
                "exec-1",
                node,
                "user-1",
                Map.of("user", Map.of("consent", false)),
                Map.of("tenantId", "tenant-a")));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("policy", "OUTREACH_COMPLIANCE")
                .containsEntry("allowed", false)
                .containsEntry("decision", "blocked")
                .containsEntry("status", "BLOCKED");
    }

    @Test
    void trimsPolicyBeforeReturningDecisionEnvelope() {
        NodeHandler handler = new OfficialRiskNodeHandler();
        DagNode node = new DagNode(
                "risk-1",
                "risk.check",
                "Winback Frequency Check",
                Map.of("policy", " WINBACK_DAILY_CAP "),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext(
                "exec-1",
                node,
                "user-1",
                Map.of(),
                Map.of()));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("policy", "WINBACK_DAILY_CAP")
                .containsEntry("allowed", true)
                .containsEntry("decision", "allowed")
                .containsEntry("status", "MATCHED");
    }

    @Test
    void defaultsSubjectToAnonymousWhenUserIdIsMissing() {
        NodeHandler handler = new OfficialRiskNodeHandler();
        DagNode node = new DagNode(
                "risk-1",
                "risk.check",
                "Winback Frequency Check",
                Map.of("policy", "WINBACK_DAILY_CAP"),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("subject", "anonymous");
    }

    @Test
    void failsWhenPolicyIsMissing() {
        NodeHandler handler = new OfficialRiskNodeHandler();
        DagNode node = new DagNode(
                "risk-1",
                "risk.check",
                "Winback Frequency Check",
                Map.of(),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("risk policy is required");
    }

    @Test
    void failsWhenPolicyIsBlank() {
        NodeHandler handler = new OfficialRiskNodeHandler();
        DagNode node = new DagNode(
                "risk-1",
                "risk.check",
                "Winback Frequency Check",
                Map.of("policy", "  "),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("risk policy is required");
    }
}
