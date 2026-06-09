package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode;
import org.chovy.canvas.domain.risk.runtime.RiskBand;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionRequest;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionResponse;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskDecisionHandlerTest {

    private final RiskDecisionService service = mock(RiskDecisionService.class);
    private final RiskDecisionHandler handler = new RiskDecisionHandler(service);

    @Test
    void allowRoutesToAllowEdge() {
        assertRoute(RiskDecisionAction.ALLOW, "node_allow");
    }

    @Test
    void reviewRoutesToReviewEdge() {
        assertRoute(RiskDecisionAction.REVIEW, "node_review");
    }

    @Test
    void verifyRoutesToVerifyEdge() {
        assertRoute(RiskDecisionAction.VERIFY, "node_verify");
    }

    @Test
    void blockRoutesToBlockEdge() {
        assertRoute(RiskDecisionAction.BLOCK, "node_block");
    }

    @Test
    void limitRoutesToLimitEdge() {
        assertRoute(RiskDecisionAction.LIMIT, "node_limit");
    }

    @Test
    void delayRoutesToDelayEdge() {
        assertRoute(RiskDecisionAction.DELAY, "node_delay");
    }

    @Test
    void shadowOnlyRoutesToAllowAndStoresSuggestedDecision() {
        when(service.evaluate(any())).thenReturn(response(RiskDecisionAction.SHADOW_ONLY));

        NodeResult result = handler.executeAsync(config().config(), context()).block();

        assertThat(result.routes()).containsEntry("ALLOW", "node_allow");
        assertThat(result.output().get("suggestedDecision")).isEqualTo("SHADOW_ONLY");
    }

    @Test
    void buildsStableRiskRequestIdFromExecutionNodeAndAttempt() {
        AtomicReference<RiskDecisionRequest> captured = new AtomicReference<>();
        when(service.evaluate(any())).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return response(RiskDecisionAction.ALLOW);
        });

        handler.executeAsync(config().config(), context()).block();

        assertThat(captured.get().requestId()).isEqualTo("exec-1:risk-node:3");
    }

    @Test
    void mapsConfiguredSubjectEventAndContextFields() {
        AtomicReference<RiskDecisionRequest> captured = new AtomicReference<>();
        when(service.evaluate(any())).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return response(RiskDecisionAction.ALLOW);
        });

        handler.executeAsync(config().config(), context()).block();

        assertThat(captured.get().tenantId()).isEqualTo(7L);
        assertThat(captured.get().subject()).containsEntry("userId", "user-secret-123");
        assertThat(captured.get().event()).containsEntry("amount", 99);
        assertThat(captured.get().context()).containsEntry("caller", "CANVAS_NODE");
        assertThat(captured.get().context()).containsEntry("businessLine", "LOYALTY");
    }

    @Test
    void missingRequiredMappingAppliesNodeFailPolicy() {
        NodeResult result = handler.executeAsync(config().withMissingSubjectMapping().config(), context()).block();

        assertThat(result.routes()).containsEntry("REVIEW", "node_review");
        assertThat(result.output().get("decision")).isEqualTo("REVIEW");
        assertThat(result.output().get("reason")).isEqualTo("MISSING_REQUIRED_MAPPING");
    }

    @Test
    void handlerDoesNotLogRawPii() {
        when(service.evaluate(any())).thenReturn(response(RiskDecisionAction.BLOCK));

        NodeResult result = handler.executeAsync(config().config(), context()).block();

        assertThat(result.output().toString()).doesNotContain("user-secret-123");
    }

    @Test
    void nodeTypeConstantIsRegistered() {
        assertThat(NodeType.RISK_DECISION).isEqualTo("RISK_DECISION");
    }

    private void assertRoute(RiskDecisionAction action, String expectedNodeId) {
        when(service.evaluate(any())).thenReturn(response(action));

        NodeResult result = handler.executeAsync(config().config(), context()).block();

        assertThat(result.success()).isTrue();
        assertThat(result.routes()).containsEntry(action.name(), expectedNodeId);
        assertThat(result.output().get("decision")).isEqualTo(action.name());
    }

    private RiskDecisionResponse response(RiskDecisionAction action) {
        return new RiskDecisionResponse(
                "exec-1:risk-node:3",
                "rd-1",
                "MARKETING_BENEFIT_ISSUE",
                "benefit_default",
                12,
                RiskRuntimeMode.ENFORCE,
                action,
                80,
                RiskBand.MEDIUM,
                List.of("reason"),
                List.of("group:rule"),
                List.of("LABEL"),
                List.of(),
                7,
                true);
    }

    private RiskDecisionNodeConfig config() {
        return RiskDecisionNodeConfig.defaultConfig();
    }

    private ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setTenantId(7L);
        ctx.setCanvasId(42L);
        ctx.setExecutionId("exec-1");
        ctx.setUserId("actor-1");
        ctx.putTriggerPayloadValues(Map.of(
                "profile", Map.of("userId", "user-secret-123"),
                "event", Map.of("amount", 99, "currency", "USD"),
                "canvas", Map.of("businessLine", "LOYALTY")));
        return ctx;
    }

    private record RiskDecisionNodeConfig(Map<String, Object> config) {
        private static RiskDecisionNodeConfig defaultConfig() {
            return new RiskDecisionNodeConfig(Map.of(
                    "nodeId", "risk-node",
                    "attempt", 3,
                    "sceneKey", "MARKETING_BENEFIT_ISSUE",
                    "subjectMapping", Map.of("userId", "$.profile.userId"),
                    "eventMapping", Map.of("amount", "$.event.amount", "currency", "$.event.currency"),
                    "contextMapping", Map.of("businessLine", "$.canvas.businessLine", "caller", "CANVAS_NODE"),
                    "actionRoutes", Map.of(
                            "ALLOW", "node_allow",
                            "REVIEW", "node_review",
                            "VERIFY", "node_verify",
                            "BLOCK", "node_block",
                            "LIMIT", "node_limit",
                            "DELAY", "node_delay"),
                    "failPolicy", "FAIL_REVIEW",
                    "timeoutMs", 50,
                    "includeTrace", false));
        }

        private RiskDecisionNodeConfig withMissingSubjectMapping() {
            Map<String, Object> copy = new java.util.LinkedHashMap<>(config);
            copy.put("subjectMapping", Map.of("userId", "$.profile.missing"));
            return new RiskDecisionNodeConfig(copy);
        }
    }
}
