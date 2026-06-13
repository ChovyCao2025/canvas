package org.chovy.canvas.web.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.risk.api.RiskDecisionCommand;
import org.chovy.canvas.risk.api.RiskDecisionFacade;
import org.chovy.canvas.risk.api.RiskDecisionView;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionReplayMismatchException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class RiskDecisionControllerCompatibilityTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-08T10:00:00Z"),
            ZoneOffset.UTC);

    @Test
    void evaluatePreservesSuccessEnvelopeAndMapsCommandWithTenantHeaderOverride() {
        CapturingRiskDecisionFacade facade = new CapturingRiskDecisionFacade(decisionView());

        webClient(facade)
                .post()
                .uri("/canvas/risk/decisions/evaluate")
                .header("X-Tenant-Id", "42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validEvaluateRequestJson(45))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.requestId").isEqualTo("risk-req-1")
                .jsonPath("$.data.decisionRunId").isEqualTo("rd-1")
                .jsonPath("$.data.sceneKey").isEqualTo("MARKETING_BENEFIT_ISSUE")
                .jsonPath("$.data.strategyKey").isEqualTo("benefit_default")
                .jsonPath("$.data.strategyVersion").isEqualTo(12)
                .jsonPath("$.data.mode").isEqualTo("ENFORCE")
                .jsonPath("$.data.decision").isEqualTo("BLOCK")
                .jsonPath("$.data.score").isEqualTo(90)
                .jsonPath("$.data.riskBand").isEqualTo("HIGH")
                .jsonPath("$.data.reasons[0]").isEqualTo("score-high")
                .jsonPath("$.data.matchedRules[0]").isEqualTo("velocity:score-high")
                .jsonPath("$.data.labels[0]").isEqualTo("VELOCITY")
                .jsonPath("$.data.missingFeatures[0]").isEqualTo("ip_reputation")
                .jsonPath("$.data.traceAvailable").isEqualTo(true)
                .jsonPath("$.data.latencyMs").isEqualTo(9);

        assertThat(facade.commands).hasSize(1);
        RiskDecisionCommand command = facade.commands.getFirst();
        assertThat(command.tenantId()).isEqualTo(42L);
        assertThat(command.requestId()).isEqualTo("risk-req-1");
        assertThat(command.sceneKey()).isEqualTo("MARKETING_BENEFIT_ISSUE");
        assertThat(command.eventTime()).isEqualTo(Instant.parse("2026-06-08T09:59:00Z"));
        assertThat(command.subject()).containsEntry("userId", "user-123")
                .containsEntry("email", "user@example.com");
        assertThat(command.event()).containsEntry("amount", 100)
                .containsEntry("eventType", "BENEFIT_ISSUE");
        assertThat(command.context()).containsEntry("caller", "CANVAS_NODE")
                .containsEntry("canvasId", 42);
        assertThat(command.features()).containsEntry("risk.score", 90);
        assertThat(command.deadlineMs()).isEqualTo(45);
    }

    @Test
    void evaluateUsesDefaultTenantAndDefaultDeadlineIgnoringBodyTenantId() {
        CapturingRiskDecisionFacade facade = new CapturingRiskDecisionFacade(decisionView());

        webClient(facade)
                .post()
                .uri("/canvas/risk/decisions/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validEvaluateRequestJson(null))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success");

        assertThat(facade.commands).hasSize(1);
        RiskDecisionCommand command = facade.commands.getFirst();
        assertThat(command.tenantId()).isEqualTo(7L);
        assertThat(command.deadlineMs()).isEqualTo(50);
    }

    @Test
    void evaluateRejectsMissingSceneKeyBeforeFacadeCall() {
        CapturingRiskDecisionFacade facade = new CapturingRiskDecisionFacade(decisionView());

        webClient(facade)
                .post()
                .uri("/canvas/risk/decisions/evaluate")
                .header("X-Tenant-Id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "requestId": "risk-req-no-scene",
                          "subject": {"userId": "user-123"},
                          "eventTime": "2026-06-08T09:59:00Z"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("sceneKey is required");

        assertThat(facade.commands).isEmpty();
    }

    @Test
    void evaluateRejectsMissingSubjectIdentifierBeforeFacadeCall() {
        CapturingRiskDecisionFacade facade = new CapturingRiskDecisionFacade(decisionView());

        webClient(facade)
                .post()
                .uri("/canvas/risk/decisions/evaluate")
                .header("X-Tenant-Id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "requestId": "risk-req-no-subject",
                          "sceneKey": "MARKETING_BENEFIT_ISSUE",
                          "subject": {"nickname": "neo"},
                          "eventTime": "2026-06-08T09:59:00Z"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("subject identifier is required");

        assertThat(facade.commands).isEmpty();
    }

    @Test
    void evaluateRejectsEventTimeMoreThanTwentyFourHoursInFutureBeforeFacadeCall() {
        CapturingRiskDecisionFacade facade = new CapturingRiskDecisionFacade(decisionView());

        webClient(facade)
                .post()
                .uri("/canvas/risk/decisions/evaluate")
                .header("X-Tenant-Id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "requestId": "risk-req-future",
                          "sceneKey": "MARKETING_BENEFIT_ISSUE",
                          "subject": {"userId": "user-123"},
                          "eventTime": "2026-06-09T10:00:01Z"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("eventTime must not be more than 24 hours in the future");

        assertThat(facade.commands).isEmpty();
    }

    @Test
    void evaluateMapsReplayMismatchToConflictEnvelope() {
        CapturingRiskDecisionFacade facade = new CapturingRiskDecisionFacade(decisionView());
        facade.replayMismatch = true;

        webClient(facade)
                .post()
                .uri("/canvas/risk/decisions/evaluate")
                .header("X-Tenant-Id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validEvaluateRequestJson(45))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.code").isEqualTo(409)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("risk decision request replay mismatch: risk-req-1");

        assertThat(facade.commands).hasSize(1);
    }

    private static WebTestClient webClient(RiskDecisionFacade facade) {
        return WebTestClient.bindToController(new RiskDecisionController(facade, CLOCK)).build();
    }

    private static RiskDecisionView decisionView() {
        return new RiskDecisionView(
                "risk-req-1",
                "rd-1",
                "MARKETING_BENEFIT_ISSUE",
                "benefit_default",
                12,
                "ENFORCE",
                "BLOCK",
                90,
                "HIGH",
                List.of("score-high"),
                List.of("velocity:score-high"),
                List.of("VELOCITY", "baseline:ALLOW", "candidate:BLOCK", "mode:ENFORCE"),
                List.of("ip_reputation"),
                true,
                9);
    }

    private static String validEvaluateRequestJson(Integer deadlineMs) {
        String options = deadlineMs == null ? "" : """
                  "options": {"modeOverride": "ENFORCE", "includeTrace": true, "deadlineMs": %d},
                """.formatted(deadlineMs);
        return """
                {
                  "tenantId": 999,
                  "requestId": "risk-req-1",
                  "sceneKey": "MARKETING_BENEFIT_ISSUE",
                %s  "subject": {"userId": "user-123", "email": "user@example.com"},
                  "eventTime": "2026-06-08T09:59:00Z",
                  "event": {"amount": 100, "eventType": "BENEFIT_ISSUE"},
                  "context": {"caller": "CANVAS_NODE", "canvasId": 42},
                  "features": {"risk.score": 90}
                }
                """.formatted(options);
    }

    private static final class CapturingRiskDecisionFacade implements RiskDecisionFacade {
        private final RiskDecisionView response;
        private final List<RiskDecisionCommand> commands = new ArrayList<>();
        private boolean replayMismatch;

        private CapturingRiskDecisionFacade(RiskDecisionView response) {
            this.response = response;
        }

        @Override
        public RiskDecisionView evaluate(RiskDecisionCommand command) {
            commands.add(command);
            if (replayMismatch) {
                throw new RiskDecisionReplayMismatchException(command.requestId());
            }
            return response;
        }
    }
}
