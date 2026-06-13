package org.chovy.canvas.web.compat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.risk.api.RiskDecisionCommand;
import org.chovy.canvas.risk.api.RiskDecisionFacade;
import org.chovy.canvas.risk.api.RiskDecisionView;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionReplayMismatchException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

class RiskApiCompatibilityTest {

    private static final Long TENANT_ID = 7L;
    private static final String ACTOR = "compat-risk";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-08T10:00:00Z"),
            ZoneOffset.UTC);

    @Test
    void evaluateRoutePreservesSuccessEnvelopeTenantOverrideAndBodyFieldMapping() {
        CapturingRiskDecisionFacade facade = new CapturingRiskDecisionFacade(decisionView());
        RecordingAuditSink auditSink = new RecordingAuditSink();

        webClient(facade, auditSink, new RecordingTraceReader())
                .post()
                .uri("/canvas/risk/decisions/evaluate")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validEvaluateRequestJson())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
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
        assertThat(command.tenantId()).isEqualTo(TENANT_ID);
        assertThat(command.requestId()).isEqualTo("risk-req-1");
        assertThat(command.sceneKey()).isEqualTo("MARKETING_BENEFIT_ISSUE");
        assertThat(command.eventTime()).isEqualTo(Instant.parse("2026-06-08T09:59:00Z"));
        assertThat(command.subject()).containsEntry("userId", "user-123");
        assertThat(command.event()).containsEntry("amount", 100);
        assertThat(command.context()).containsEntry("caller", "CANVAS_NODE");
        assertThat(command.features()).containsEntry("risk.score", 90);
        assertThat(command.deadlineMs()).isEqualTo(45);
        assertThat(auditSink.warnings)
                .containsExactly("ignored body tenantId=999 for actor=compat-risk tenant=7");
    }

    @Test
    void evaluateRouteRejectsMissingSceneKeyBeforeCallingFacade() {
        CapturingRiskDecisionFacade facade = new CapturingRiskDecisionFacade(decisionView());

        expectBadRequest(facade, evaluateRequestWithoutSceneKey());

        assertThat(facade.commands).isEmpty();
    }

    @Test
    void evaluateRouteRejectsMissingSubjectIdentifierBeforeCallingFacade() {
        CapturingRiskDecisionFacade facade = new CapturingRiskDecisionFacade(decisionView());

        expectBadRequest(facade, evaluateRequestJson("risk-req-no-subject",
                "MARKETING_BENEFIT_ISSUE",
                "{\"nickname\":\"neo\"}",
                "2026-06-08T09:59:00Z",
                45));

        assertThat(facade.commands).isEmpty();
    }

    @Test
    void evaluateRouteRejectsFutureEventTimeBeforeCallingFacade() {
        CapturingRiskDecisionFacade facade = new CapturingRiskDecisionFacade(decisionView());

        expectBadRequest(facade, evaluateRequestJson("risk-req-future",
                "MARKETING_BENEFIT_ISSUE",
                "{\"userId\":\"user-123\"}",
                "2026-06-09T10:00:01Z",
                45));

        assertThat(facade.commands).isEmpty();
    }

    @Test
    void evaluateRouteRejectsDeadlineAboveSceneBudgetBeforeCallingFacade() {
        CapturingRiskDecisionFacade facade = new CapturingRiskDecisionFacade(decisionView());
        RecordingBudgetProvider budgetProvider = new RecordingBudgetProvider(50);

        webClient(facade, new RecordingAuditSink(), new RecordingTraceReader(), budgetProvider)
                .post()
                .uri("/canvas/risk/decisions/evaluate")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(evaluateRequestJson("risk-req-deadline",
                        "MARKETING_BENEFIT_ISSUE",
                        "{\"userId\":\"user-123\"}",
                        "2026-06-08T09:59:00Z",
                        51))
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(facade.commands).isEmpty();
        assertThat(budgetProvider.requests)
                .containsExactly(new BudgetRequest(TENANT_ID, "MARKETING_BENEFIT_ISSUE"));
    }

    @Test
    void evaluateRouteMapsReplayMismatchToConflict() {
        CapturingRiskDecisionFacade facade = new CapturingRiskDecisionFacade(decisionView());
        facade.replayMismatch = true;

        webClient(facade, new RecordingAuditSink(), new RecordingTraceReader())
                .post()
                .uri("/canvas/risk/decisions/evaluate")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validEvaluateRequestJson())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        assertThat(facade.commands).hasSize(1);
    }

    @Test
    void traceRoutePreservesEnvelopeAndSceneKeyLimitQueryHandling() {
        RecordingTraceReader traceReader = new RecordingTraceReader(List.of(new RiskDecisionTraceView(
                "rd-1",
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
                List.of("velocity:score-high"))));

        webClient(new CapturingRiskDecisionFacade(decisionView()), new RecordingAuditSink(), traceReader)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/risk/decisions/traces")
                        .queryParam("sceneKey", "MARKETING_BENEFIT_ISSUE")
                        .queryParam("limit", 25)
                        .build())
                .header("X-Tenant-Id", TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].traceId").isEqualTo("rd-1")
                .jsonPath("$.data[0].requestId").isEqualTo("risk-req-1")
                .jsonPath("$.data[0].sceneKey").isEqualTo("MARKETING_BENEFIT_ISSUE")
                .jsonPath("$.data[0].decision").isEqualTo("BLOCK")
                .jsonPath("$.data[0].matchedRules[0]").isEqualTo("velocity:score-high");

        assertThat(traceReader.requests)
                .containsExactly(new TraceRequest(TENANT_ID, "MARKETING_BENEFIT_ISSUE", 25));
    }

    private static void expectBadRequest(CapturingRiskDecisionFacade facade, String body) {
        webClient(facade, new RecordingAuditSink(), new RecordingTraceReader())
                .post()
                .uri("/canvas/risk/decisions/evaluate")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest();
    }

    private static WebTestClient webClient(RiskDecisionFacade facade,
                                           RecordingAuditSink auditSink,
                                           RecordingTraceReader traceReader) {
        return webClient(facade, auditSink, traceReader, new RecordingBudgetProvider(50));
    }

    private static WebTestClient webClient(RiskDecisionFacade facade,
                                           RecordingAuditSink auditSink,
                                           RecordingTraceReader traceReader,
                                           RecordingBudgetProvider budgetProvider) {
        return WebTestClient.bindToController(new RiskDecisionControllerAdapter(
                        facade,
                        auditSink,
                        budgetProvider,
                        traceReader,
                        CLOCK))
                .build();
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

    private static String validEvaluateRequestJson() {
        return evaluateRequestJson("risk-req-1",
                "MARKETING_BENEFIT_ISSUE",
                "{\"userId\":\"user-123\",\"email\":\"user@example.com\"}",
                "2026-06-08T09:59:00Z",
                45);
    }

    private static String evaluateRequestJson(String requestId,
                                              String sceneKey,
                                              String subjectJson,
                                              String eventTime,
                                              int deadlineMs) {
        return """
                {
                  "tenantId": 999,
                  "requestId": "%s",
                  "sceneKey": "%s",
                  "subject": %s,
                  "eventTime": "%s",
                  "event": {"amount": 100, "eventType": "BENEFIT_ISSUE"},
                  "context": {"caller": "CANVAS_NODE", "canvasId": 42},
                  "features": {"risk.score": 90},
                  "options": {"modeOverride": "ENFORCE", "includeTrace": true, "deadlineMs": %d}
                }
                """.formatted(requestId, sceneKey, subjectJson, eventTime, deadlineMs);
    }

    private static String evaluateRequestWithoutSceneKey() {
        return """
                {
                  "tenantId": 999,
                  "requestId": "risk-req-no-scene",
                  "subject": {"userId": "user-123"},
                  "eventTime": "2026-06-08T09:59:00Z",
                  "event": {"amount": 100, "eventType": "BENEFIT_ISSUE"},
                  "context": {"caller": "CANVAS_NODE", "canvasId": 42},
                  "features": {"risk.score": 90},
                  "options": {"modeOverride": "ENFORCE", "includeTrace": true, "deadlineMs": 45}
                }
                """;
    }

    @RestController
    private static final class RiskDecisionControllerAdapter {
        private static final int DEFAULT_DEADLINE_MS = 50;

        private final RiskDecisionFacade facade;
        private final RiskDecisionAuditSink auditSink;
        private final RiskSceneBudgetProvider budgetProvider;
        private final RiskDecisionTraceReader traceReader;
        private final Clock clock;

        private RiskDecisionControllerAdapter(RiskDecisionFacade facade,
                                              RiskDecisionAuditSink auditSink,
                                              RiskSceneBudgetProvider budgetProvider,
                                              RiskDecisionTraceReader traceReader,
                                              Clock clock) {
            this.facade = facade;
            this.auditSink = auditSink;
            this.budgetProvider = budgetProvider;
            this.traceReader = traceReader;
            this.clock = clock;
        }

        @PostMapping("/canvas/risk/decisions/evaluate")
        Mono<CompatibilityEnvelope<RiskDecisionView>> evaluate(
                @RequestHeader(name = "X-Tenant-Id", required = false) Long tenantId,
                @RequestBody(required = false) EvaluateRequest body) {
            return Mono.fromCallable(() -> {
                        Long authenticatedTenantId = authenticatedTenantId(tenantId);
                        EvaluateRequest request = body == null ? EvaluateRequest.empty() : body;
                        validate(authenticatedTenantId, request);
                        return CompatibilityEnvelope.ok(facade.evaluate(toCommand(authenticatedTenantId, request)));
                    })
                    .onErrorMap(RiskDecisionReplayMismatchException.class,
                            error -> new ResponseStatusException(HttpStatus.CONFLICT, error.getMessage(), error));
        }

        @GetMapping("/canvas/risk/decisions/traces")
        Mono<CompatibilityEnvelope<List<RiskDecisionTraceView>>> listTraces(
                @RequestHeader(name = "X-Tenant-Id", required = false) Long tenantId,
                @RequestParam(required = false) String sceneKey,
                @RequestParam(defaultValue = "50") int limit) {
            return Mono.just(CompatibilityEnvelope.ok(
                    traceReader.listTraces(authenticatedTenantId(tenantId), sceneKey, limit)));
        }

        private Long authenticatedTenantId(Long tenantId) {
            return tenantId == null ? TENANT_ID : tenantId;
        }

        private void validate(Long tenantId, EvaluateRequest request) {
            if (isBlank(request.requestId())) {
                throw badRequest("requestId is required");
            }
            if (isBlank(request.sceneKey())) {
                throw badRequest("sceneKey is required");
            }
            if (!hasSubjectIdentifier(request.subject())) {
                throw badRequest("subject identifier is required");
            }
            Instant eventTime = parseEventTime(request.eventTime());
            if (eventTime.isAfter(clock.instant().plusSeconds(24 * 60 * 60))) {
                throw badRequest("eventTime must not be more than 24 hours in the future");
            }
            int deadlineMs = deadlineMs(request);
            int sceneBudget = budgetProvider.latencyBudgetMs(tenantId, request.sceneKey());
            if (deadlineMs > sceneBudget) {
                throw badRequest("deadline must not exceed scene latency budget");
            }
            if (request.tenantId() != null && !request.tenantId().equals(tenantId)) {
                auditSink.tenantOverrideIgnored(tenantId, request.tenantId(), ACTOR);
            }
        }

        private RiskDecisionCommand toCommand(Long tenantId, EvaluateRequest request) {
            return new RiskDecisionCommand(
                    tenantId,
                    request.requestId(),
                    request.sceneKey(),
                    parseEventTime(request.eventTime()),
                    request.subject(),
                    request.event(),
                    request.context(),
                    request.features(),
                    deadlineMs(request));
        }

        private int deadlineMs(EvaluateRequest request) {
            if (request.options() == null || request.options().deadlineMs() == null) {
                return DEFAULT_DEADLINE_MS;
            }
            return request.options().deadlineMs();
        }

        private Instant parseEventTime(String eventTime) {
            if (isBlank(eventTime)) {
                throw badRequest("eventTime is required");
            }
            try {
                return Instant.parse(eventTime);
            } catch (DateTimeParseException error) {
                throw badRequest("eventTime must be ISO-8601");
            }
        }

        private boolean hasSubjectIdentifier(Map<String, Object> subject) {
            return hasText(subject, "userId")
                    || hasText(subject, "deviceId")
                    || hasText(subject, "ip")
                    || hasText(subject, "email")
                    || hasText(subject, "phone");
        }

        private boolean hasText(Map<String, Object> source, String key) {
            Object value = source == null ? null : source.get(key);
            return value != null && !value.toString().isBlank();
        }

        private boolean isBlank(String value) {
            return value == null || value.isBlank();
        }

        private ResponseStatusException badRequest(String message) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private record EvaluateRequest(
            Long tenantId,
            String requestId,
            String sceneKey,
            Map<String, Object> subject,
            String eventTime,
            Map<String, Object> event,
            Map<String, Object> context,
            Map<String, Object> features,
            Options options) {

        private EvaluateRequest {
            subject = subject == null ? Map.of() : new LinkedHashMap<>(subject);
            event = event == null ? Map.of() : new LinkedHashMap<>(event);
            context = context == null ? Map.of() : new LinkedHashMap<>(context);
            features = features == null ? Map.of() : new LinkedHashMap<>(features);
        }

        private static EvaluateRequest empty() {
            return new EvaluateRequest(null, null, null, Map.of(), null, Map.of(), Map.of(), Map.of(), null);
        }
    }

    private record Options(
            String modeOverride,
            boolean includeTrace,
            Integer deadlineMs) {
    }

    private record RiskDecisionTraceView(
            String traceId,
            String requestId,
            String sceneKey,
            String strategyKey,
            Integer strategyVersion,
            String mode,
            String decision,
            Integer score,
            String riskBand,
            Integer latencyMs,
            String createdAt,
            List<String> matchedRules) {
    }

    private record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }
    }

    private interface RiskDecisionAuditSink {
        void tenantOverrideIgnored(Long authenticatedTenantId, Long bodyTenantId, String actor);
    }

    private interface RiskSceneBudgetProvider {
        int latencyBudgetMs(Long tenantId, String sceneKey);
    }

    private interface RiskDecisionTraceReader {
        List<RiskDecisionTraceView> listTraces(Long tenantId, String sceneKey, int limit);
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

    private static final class RecordingAuditSink implements RiskDecisionAuditSink {
        private final List<String> warnings = new ArrayList<>();

        @Override
        public void tenantOverrideIgnored(Long authenticatedTenantId, Long bodyTenantId, String actor) {
            warnings.add("ignored body tenantId=" + bodyTenantId
                    + " for actor=" + actor
                    + " tenant=" + authenticatedTenantId);
        }
    }

    private static final class RecordingBudgetProvider implements RiskSceneBudgetProvider {
        private final int budgetMs;
        private final List<BudgetRequest> requests = new ArrayList<>();

        private RecordingBudgetProvider(int budgetMs) {
            this.budgetMs = budgetMs;
        }

        @Override
        public int latencyBudgetMs(Long tenantId, String sceneKey) {
            requests.add(new BudgetRequest(tenantId, sceneKey));
            return budgetMs;
        }
    }

    private static final class RecordingTraceReader implements RiskDecisionTraceReader {
        private final List<RiskDecisionTraceView> traces;
        private final List<TraceRequest> requests = new ArrayList<>();

        private RecordingTraceReader() {
            this(List.of());
        }

        private RecordingTraceReader(List<RiskDecisionTraceView> traces) {
            this.traces = traces;
        }

        @Override
        public List<RiskDecisionTraceView> listTraces(Long tenantId, String sceneKey, int limit) {
            requests.add(new TraceRequest(tenantId, sceneKey, limit));
            return traces;
        }
    }

    private record BudgetRequest(Long tenantId, String sceneKey) {
    }

    private record TraceRequest(Long tenantId, String sceneKey, int limit) {
    }
}
