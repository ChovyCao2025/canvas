package org.chovy.canvas.web.risk;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode;
import org.chovy.canvas.domain.risk.runtime.RiskBand;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionReplayMismatchException;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionService;
import org.chovy.canvas.web.risk.dto.RiskDecisionEvaluateRequest;
import org.chovy.canvas.web.risk.dto.RiskDecisionEvaluateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskDecisionControllerTest {

    private final RiskDecisionService service = mock(RiskDecisionService.class);
    private final TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
    private final RecordingAuditSink auditSink = new RecordingAuditSink();
    private final RecordingTraceReader traceReader = new RecordingTraceReader();
    private final RiskSceneBudgetProvider budgetProvider = (tenantId, sceneKey) -> 50;
    private final RiskDecisionController controller = new RiskDecisionController(
            service,
            tenantResolver,
            auditSink,
            budgetProvider,
            Clock.fixed(Instant.parse("2026-06-08T10:00:00Z"), ZoneOffset.UTC),
            traceReader);

    @Test
    void usesAuthenticatedTenantContext() {
        tenant(RoleNames.OPERATOR);
        when(service.evaluate(any())).thenReturn(runtimeResponse());

        StepVerifier.create(controller.evaluate(request().withTenantId(999L)).map(response -> response.getData()))
                .assertNext(response -> assertThat(response.requestId()).isEqualTo("risk-req-1"))
                .verifyComplete();

        verify(service).evaluate(org.mockito.ArgumentMatchers.argThat(command -> command.tenantId().equals(7L)));
    }

    @Test
    void ignoresBodyTenantIdAndRecordsAuditWarning() {
        tenant(RoleNames.OPERATOR);
        when(service.evaluate(any())).thenReturn(runtimeResponse());

        StepVerifier.create(controller.evaluate(request().withTenantId(999L)))
                .assertNext(response -> assertThat(response.getCode()).isZero())
                .verifyComplete();

        assertThat(auditSink.warnings).containsExactly("ignored body tenantId=999 for actor=alice tenant=7");
    }

    @Test
    void rejectsBlankSceneKey() {
        tenant(RoleNames.OPERATOR);

        StepVerifier.create(controller.evaluate(request().withSceneKey(" ")))
                .expectErrorSatisfies(error -> assertBadRequest(error, "sceneKey"))
                .verify();
    }

    @Test
    void rejectsMissingSubjectIdentifier() {
        tenant(RoleNames.OPERATOR);

        StepVerifier.create(controller.evaluate(request().withSubject(Map.of("nickname", "neo"))))
                .expectErrorSatisfies(error -> assertBadRequest(error, "subject"))
                .verify();
    }

    @Test
    void rejectsEventTimeMoreThanTwentyFourHoursInFuture() {
        tenant(RoleNames.OPERATOR);

        StepVerifier.create(controller.evaluate(request().withEventTime("2026-06-09T10:00:01Z")))
                .expectErrorSatisfies(error -> assertBadRequest(error, "eventTime"))
                .verify();
    }

    @Test
    void rejectsDeadlineAboveSceneBudget() {
        tenant(RoleNames.OPERATOR);

        StepVerifier.create(controller.evaluate(request().withDeadlineMs(51)))
                .expectErrorSatisfies(error -> assertBadRequest(error, "deadline"))
                .verify();
    }

    @Test
    void returnsDecisionScoreBandReasonsAndMatchedRules() {
        tenant(RoleNames.OPERATOR);
        when(service.evaluate(any())).thenReturn(runtimeResponse());

        StepVerifier.create(controller.evaluate(request()).map(response -> response.getData()))
                .assertNext(response -> {
                    assertThat(response.requestId()).isEqualTo("risk-req-1");
                    assertThat(response.decisionRunId()).isEqualTo("rd-1");
                    assertThat(response.sceneKey()).isEqualTo("MARKETING_BENEFIT_ISSUE");
                    assertThat(response.strategyKey()).isEqualTo("benefit_default");
                    assertThat(response.strategyVersion()).isEqualTo(12);
                    assertThat(response.mode()).isEqualTo("ENFORCE");
                    assertThat(response.decision()).isEqualTo("BLOCK");
                    assertThat(response.score()).isEqualTo(90);
                    assertThat(response.riskBand()).isEqualTo("HIGH");
                    assertThat(response.reasons()).containsExactly("score-high");
                    assertThat(response.matchedRules()).containsExactly("velocity:score-high");
                    assertThat(response.labels()).containsExactly("VELOCITY");
                    assertThat(response.missingFeatures()).isEmpty();
                    assertThat(response.traceAvailable()).isTrue();
                    assertThat(response.latencyMs()).isEqualTo(9);
                })
                .verifyComplete();
    }

    @Test
    void listsDecisionTracesForWorkbenchReads() {
        tenant(RoleNames.OPERATOR);
        traceReader.traces = List.of(new RiskDecisionTraceView(
                "42",
                "risk-req-1",
                "MARKETING_BENEFIT_ISSUE",
                "benefit_default",
                12,
                "ENFORCE",
                "BLOCK",
                90,
                "HIGH",
                9,
                "2026-06-08T10:00:00Z",
                List.of("velocity:block")));

        StepVerifier.create(controller.listTraces("MARKETING_BENEFIT_ISSUE", 20).map(response -> response.getData()))
                .assertNext(traces -> {
                    assertThat(traces).hasSize(1);
                    assertThat(traces.getFirst().traceId()).isEqualTo("42");
                    assertThat(traces.getFirst().decision()).isEqualTo("BLOCK");
                    assertThat(traceReader.requests).containsExactly("7:MARKETING_BENEFIT_ISSUE:20");
                })
                .verifyComplete();
    }

    @Test
    void mapsReplayMismatchToConflict() {
        tenant(RoleNames.OPERATOR);
        when(service.evaluate(any())).thenThrow(new RiskDecisionReplayMismatchException("risk-req-1"));

        StepVerifier.create(controller.evaluate(request()))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                })
                .verify();
    }

    @Test
    void requiresRiskDecisionEvaluatePermission() {
        tenant("VIEWER");

        StepVerifier.create(controller.evaluate(request()))
                .expectError(AccessDeniedException.class)
                .verify();
    }

    private RiskDecisionEvaluateRequest request() {
        return new RiskDecisionEvaluateRequest(
                null,
                "risk-req-1",
                "MARKETING_BENEFIT_ISSUE",
                Map.of("userId", "u-1"),
                "2026-06-08T09:59:00Z",
                Map.of("amount", 100),
                Map.of("caller", "CANVAS_NODE"),
                Map.of("risk.score", 90),
                new RiskDecisionEvaluateRequest.Options(null, true, 50));
    }

    private org.chovy.canvas.domain.risk.runtime.RiskDecisionResponse runtimeResponse() {
        return new org.chovy.canvas.domain.risk.runtime.RiskDecisionResponse(
                "risk-req-1",
                "rd-1",
                "MARKETING_BENEFIT_ISSUE",
                "benefit_default",
                12,
                RiskRuntimeMode.ENFORCE,
                RiskDecisionAction.BLOCK,
                90,
                RiskBand.HIGH,
                List.of("score-high"),
                List.of("velocity:score-high"),
                List.of("VELOCITY"),
                List.of(),
                9,
                true);
    }

    private void tenant(String role) {
        when(tenantResolver.current()).thenReturn(Mono.just(new TenantContext(7L, role, "alice")));
    }

    private void assertBadRequest(Throwable error, String messageFragment) {
        assertThat(error).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException status = (ResponseStatusException) error;
        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(status.getReason()).contains(messageFragment);
    }

    private static final class RecordingAuditSink implements RiskDecisionAuditSink {
        private final List<String> warnings = new ArrayList<>();

        @Override
        public void tenantOverrideIgnored(Long authenticatedTenantId, Long bodyTenantId, String actor) {
            warnings.add("ignored body tenantId=" + bodyTenantId
                    + " for actor=" + actor
                    + " tenant=" + authenticatedTenantId);
        }
    }

    private static final class RecordingTraceReader implements RiskDecisionTraceReader {
        private List<RiskDecisionTraceView> traces = List.of();
        private final List<String> requests = new ArrayList<>();

        @Override
        public List<RiskDecisionTraceView> listTraces(Long tenantId, String sceneKey, int limit) {
            requests.add(tenantId + ":" + sceneKey + ":" + limit);
            return traces;
        }
    }
}
