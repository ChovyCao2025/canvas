package org.chovy.canvas.web.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.platform.api.AiFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/ai")
public class AiController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final AiFacade facade;

    public AiController(AiFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/decisions/recompute")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> recomputeDecision(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.recomputeDecision(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/decisions/latest-run")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> latestDecisionRun(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String decisionScope) {
        return envelope(() -> facade.latestDecisionRun(tenantIdOrDefault(tenantId), decisionScope));
    }

    @GetMapping("/decisions/recommendations")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> decisionRecommendations(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long runId,
            @RequestParam(required = false) String decisionType,
            @RequestParam(required = false) String eligibilityStatus,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.decisionRecommendations(tenantIdOrDefault(tenantId), runId, decisionType,
                eligibilityStatus, limit));
    }

    @PostMapping("/decisions/recommendations/{recommendationId}/feedback")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> recordDecisionFeedback(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long recommendationId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.recordDecisionFeedback(tenantIdOrDefault(tenantId), recommendationId,
                safePayload(payload), actorOrDefault(actor)));
    }

    @GetMapping("/predictions/latest-run")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> latestPredictionRun(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.latestPredictionRun(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/predictions/readiness")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> predictionReadiness(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.predictionReadiness(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/predictions/churn-distribution")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> churnDistribution(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.churnDistribution(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/predictions/top-risk-users")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> topRiskUsers(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.topRiskUsers(tenantIdOrDefault(tenantId), limit));
    }

    @PostMapping("/predictions/recompute")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> recomputePrediction(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.recomputePrediction(tenantIdOrDefault(tenantId), safePayload(payload)));
    }

    @GetMapping("/prompt-templates")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> promptTemplates(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.promptTemplates(tenantIdOrDefault(tenantId)));
    }

    @PostMapping("/prompt-templates")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createPromptTemplate(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createPromptTemplate(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/prompt-templates/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> promptTemplate(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> facade.promptTemplate(tenantIdOrDefault(tenantId), id));
    }

    @PutMapping("/prompt-templates/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> updatePromptTemplate(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.updatePromptTemplate(tenantIdOrDefault(tenantId), id, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/prompt-templates/{id}/disable")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> disablePromptTemplate(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.disablePromptTemplate(tenantIdOrDefault(tenantId), id, actorOrDefault(actor)));
    }

    @PostMapping("/prompt-templates/render")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> renderPromptTemplate(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.renderPromptTemplate(tenantIdOrDefault(tenantId), safePayload(payload)));
    }

    @PostMapping("/prompt-templates/evaluate")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> evaluatePromptTemplate(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.evaluatePromptTemplate(tenantIdOrDefault(tenantId), safePayload(payload)));
    }

    @GetMapping("/prompt-templates/evaluation-audits")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> evaluationAudits(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.evaluationAudits(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/providers")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> providers(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.providers(tenantIdOrDefault(tenantId)));
    }

    @PostMapping("/providers")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createProvider(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createProvider(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/providers/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> provider(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> facade.provider(tenantIdOrDefault(tenantId), id));
    }

    @PutMapping("/providers/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> updateProvider(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.updateProvider(tenantIdOrDefault(tenantId), id, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/providers/{id}/disable")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> disableProvider(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.disableProvider(tenantIdOrDefault(tenantId), id, actorOrDefault(actor)));
    }

    @GetMapping("/providers/{id}/models")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> providerModels(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> facade.providerModels(tenantIdOrDefault(tenantId), id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : new LinkedHashMap<>(payload);
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

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
