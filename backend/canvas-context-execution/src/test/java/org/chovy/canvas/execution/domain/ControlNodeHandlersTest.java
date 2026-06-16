package org.chovy.canvas.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ControlNodeHandlersTest {

    @Test
    void ifConditionRoutesToSuccessWhenAllRulesMatchContext() {
        NodeExecutionResult result = new IfConditionNodeHandler().execute(context(
                node("decision", "IF_CONDITION", Map.of(
                        "rules", List.of(Map.of(
                                "field", "tier",
                                "operator", "EQ",
                                "value", "gold")),
                        "successNodeId", "vip-path",
                        "failNodeId", "standard-path")),
                Map.of(),
                Map.of("tier", "gold")));

        assertThat(result.success()).isTrue();
        assertThat(result.routes()).containsEntry("success", "vip-path");
        assertThat(result.output()).containsEntry("passed", true);
    }

    @Test
    void ifConditionRoutesToFailureWhenRuleDoesNotMatchContext() {
        NodeExecutionResult result = new IfConditionNodeHandler().execute(context(
                node("decision", "IF_CONDITION", Map.of(
                        "rules", List.of(Map.of(
                                "field", "tier",
                                "operator", "EQ",
                                "value", "gold")),
                        "successNodeId", "vip-path",
                        "failNodeId", "standard-path")),
                Map.of(),
                Map.of("tier", "silver")));

        assertThat(result.success()).isTrue();
        assertThat(result.routes()).containsEntry("fail", "standard-path");
        assertThat(result.output()).containsEntry("passed", false);
    }

    @Test
    void waitSuspendsFirstEntryWithResumePayloadShape() {
        NodeExecutionResult result = new WaitNodeHandler().execute(context(
                node("wait", "WAIT", Map.of(
                        "waitType", "DURATION",
                        "duration", Map.of("value", 5, "unit", "MINUTES"),
                        "nextNodeId", "after-wait",
                        "timeoutNodeId", "wait-timeout")),
                Map.of(),
                Map.of()));

        assertThat(result.pending()).isTrue();
        assertThat(result.output())
                .containsEntry("waitStatus", "PENDING")
                .containsEntry("waitType", "DURATION")
                .containsEntry("sourceNodeId", "wait")
                .containsEntry("successNodeId", "after-wait")
                .containsEntry("timeoutNodeId", "wait-timeout");
    }

    @Test
    void waitRoutesTimeoutResumeToTimeoutBranch() {
        NodeExecutionResult result = new WaitNodeHandler().execute(context(
                node("wait", "WAIT", Map.of(
                        "waitResumeStatus", "TIMEOUT",
                        "nextNodeId", "after-wait",
                        "timeoutNodeId", "wait-timeout")),
                Map.of(),
                Map.of()));

        assertThat(result.pending()).isFalse();
        assertThat(result.routes()).containsEntry("timeout", "wait-timeout");
        assertThat(result.output()).containsEntry("waitStatus", "TIMEOUT");
    }

    @Test
    void userInputSuspendsWithFormSchemaAndCompletionBranches() {
        NodeExecutionResult result = new UserInputNodeHandler().execute(context(
                node("collect", "USER_INPUT", Map.of(
                        "formSchema", Map.of("fields", List.of("reason")),
                        "completedNodeId", "review",
                        "timeoutNodeId", "expired")),
                Map.of(),
                Map.of()));

        assertThat(result.pending()).isTrue();
        assertThat(result.output())
                .containsEntry("inputStatus", "PENDING")
                .containsEntry("sourceNodeId", "collect")
                .containsEntry("completedNodeId", "review")
                .containsEntry("timeoutNodeId", "expired");
    }

    @Test
    void userInputRoutesCompletedResumeWithResponseOutput() {
        NodeExecutionResult result = new UserInputNodeHandler().execute(context(
                node("collect", "USER_INPUT", Map.of(
                        "formSchema", Map.of("fields", List.of("reason")),
                        "completedNodeId", "review",
                        "timeoutNodeId", "expired")),
                Map.of(
                        "waitResumeStatus", "COMPLETED",
                        "inputResponseId", "response-1",
                        "inputResponse", Map.of("reason", "approved")),
                Map.of()));

        assertThat(result.routes()).containsEntry("completed", "review");
        assertThat(result.output())
                .containsEntry("inputStatus", "COMPLETED")
                .containsEntry("inputResponseId", "response-1")
                .containsEntry("inputResponse", Map.of("reason", "approved"));
    }

    @Test
    void directCallValidatesRequiredInputAndRoutesConfiguredBranches() {
        NodeExecutionResult result = new DirectCallNodeHandler().execute(context(
                node("direct", "DIRECT_CALL", Map.of(
                        "inputParams", List.of(Map.of("name", "orderId", "required", true)),
                        "branches", List.of(
                                Map.of("nextNodeId", "fast-lane"),
                                Map.of("nextNodeId", "slow-lane")))),
                Map.of("orderId", "O-1"),
                Map.of("orderId", "O-1")));

        assertThat(result.success()).isTrue();
        assertThat(result.routes())
                .containsEntry("branch-0", "fast-lane")
                .containsEntry("branch-1", "slow-lane");
        assertThat(result.output()).containsEntry("directCallAccepted", true);
    }

    @Test
    void directCallFailsWhenRequiredInputIsMissing() {
        NodeExecutionResult result = new DirectCallNodeHandler().execute(context(
                node("direct", "DIRECT_CALL", Map.of(
                        "inputParams", List.of(Map.of("name", "orderId", "required", true)),
                        "nextNodeId", "next")),
                Map.of(),
                Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("orderId");
    }

    @Test
    void directReturnBuildsTerminalResponseFromContextAndLiterals() {
        NodeExecutionResult result = new DirectReturnNodeHandler().execute(context(
                node("return", "DIRECT_RETURN", Map.of("data", List.of(
                        Map.of("name", "orderId", "valueType", "CONTEXT", "value", "orderId"),
                        Map.of("name", "status", "valueType", "LITERAL", "value", "accepted"),
                        Map.of("name", "coupon", "value", "${couponCode}")))),
                Map.of(),
                Map.of("orderId", "O-1", "couponCode", "A10")));

        assertThat(result.success()).isTrue();
        assertThat(result.routes()).isEmpty();
        assertThat(result.output())
                .containsEntry("orderId", "O-1")
                .containsEntry("status", "accepted")
                .containsEntry("coupon", "A10");
    }

    @Test
    void splitRoutesSelectedBranchAndFallsBackToFirstConfiguredBranch() {
        NodeExecutionResult result = new SplitNodeHandler().execute(context(
                node("split", "SPLIT", Map.of("branches", List.of(
                        Map.of("branchId", "control", "nextNodeId", "control-node"),
                        Map.of("branchId", "treatment", "selected", true, "nextNodeId", "treatment-node")))),
                Map.of(),
                Map.of()));

        assertThat(result.success()).isTrue();
        assertThat(result.routes()).containsEntry("branch-treatment", "treatment-node");
        assertThat(result.output()).containsEntry("splitBranch", "treatment");
    }

    @Test
    void aggregateCountsUpstreamSuccessesAndRoutesByThreshold() {
        NodeExecutionResult result = new AggregateNodeHandler().execute(context(
                node("aggregate", "AGGREGATE", Map.of(
                        "upstreamIds", List.of("email", "sms", "push"),
                        "evaluateMode", "count",
                        "minCount", 2,
                        "successNodeId", "passed",
                        "failNodeId", "failed")),
                Map.of(),
                Map.of(
                        "nodeStatuses", Map.of(
                                "email", "SUCCESS",
                                "sms", "FAILED",
                                "push", "SUCCESS"),
                        "nodeOutputs", Map.of(
                                "email", Map.of("sent", true),
                                "push", Map.of("sent", true)))));

        assertThat(result.success()).isTrue();
        assertThat(result.routes()).containsEntry("success", "passed");
        assertThat(result.output())
                .containsEntry("successCount", 2L)
                .containsEntry("failCount", 1L)
                .containsEntry("totalCount", 3L)
                .containsEntry("passed", true);
    }

    private static NodeExecutionContext context(
            DagNode node,
            Map<String, Object> payload,
            Map<String, Object> contextData) {
        return new NodeExecutionContext("exec-1", node, "user-1", payload, contextData);
    }

    private static DagNode node(String nodeId, String nodeType, Map<String, Object> config) {
        return new DagNode(nodeId, nodeType, nodeType, config, Map.of());
    }
}
