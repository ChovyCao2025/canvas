package org.chovy.canvas.execution.adapter.plugin.official.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.domain.DagNode;
import org.chovy.canvas.execution.domain.NodeExecutionContext;
import org.chovy.canvas.execution.domain.NodeExecutionResult;
import org.chovy.canvas.execution.domain.NodeHandler;
import org.chovy.canvas.execution.domain.NodeHandlerRegistry;
import org.junit.jupiter.api.Test;

class OfficialApprovalPluginTest {

    @Test
    void registersApprovalRequestHandlerThroughExecutionRegistry() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry(List.of(new OfficialApprovalNodeHandler()));

        assertThat(registry.has("approval.request")).isTrue();
        assertThat(registry.metadata()).extracting(metadata -> metadata.nodeType())
                .containsExactly("approval.request");
    }

    @Test
    void returnsDeterministicApprovalRequestEnvelope() {
        NodeHandler handler = new OfficialApprovalNodeHandler();
        DagNode node = new DagNode(
                "approval-1",
                "approval.request",
                "High Value Coupon Approval",
                Map.of("approvalCode", "HIGH_VALUE_COUPON"),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext(
                "exec-1",
                node,
                "operator-1",
                Map.of("couponKey", "VIP_50"),
                Map.of("tenantId", "tenant-a")));

        assertThat(result.success()).isTrue();
        assertThat(result.pending()).isFalse();
        assertThat(result.error()).isEmpty();
        assertThat(result.output()).containsEntry("pluginId", "canvas-plugin-approval")
                .containsEntry("nodeType", "approval.request")
                .containsEntry("approvalCode", "HIGH_VALUE_COUPON")
                .containsEntry("requester", "operator-1")
                .containsEntry("request", "stub")
                .containsEntry("status", "APPROVED");
        assertThat(result.output().get("payload")).isEqualTo(Map.of("couponKey", "VIP_50"));
        assertThat(result.output().get("context")).isEqualTo(Map.of("tenantId", "tenant-a"));
    }

    @Test
    void trimsApprovalCodeAndDefaultsRequesterToAnonymousWhenUserIdIsMissing() {
        NodeHandler handler = new OfficialApprovalNodeHandler();
        DagNode node = new DagNode(
                "approval-1",
                "approval.request",
                "High Value Coupon Approval",
                Map.of("approvalCode", " HIGH_VALUE_COUPON "),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("approvalCode", "HIGH_VALUE_COUPON")
                .containsEntry("requester", "anonymous");
    }

    @Test
    void failsWhenApprovalCodeIsMissing() {
        NodeHandler handler = new OfficialApprovalNodeHandler();
        DagNode node = new DagNode(
                "approval-1",
                "approval.request",
                "High Value Coupon Approval",
                Map.of(),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("approval code is required");
    }

    @Test
    void failsWhenApprovalCodeIsBlank() {
        NodeHandler handler = new OfficialApprovalNodeHandler();
        DagNode node = new DagNode(
                "approval-1",
                "approval.request",
                "High Value Coupon Approval",
                Map.of("approvalCode", "  "),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("approval code is required");
    }

}
