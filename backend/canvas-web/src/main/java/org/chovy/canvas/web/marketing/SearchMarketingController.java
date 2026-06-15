package org.chovy.canvas.web.marketing;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.SearchMarketingFacade;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/canvas/search-marketing")
public class SearchMarketingController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final SearchMarketingFacade facade;

    public SearchMarketingController(SearchMarketingFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/sources")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> sources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listSources(tenantIdOrDefault(tenantId), provider, channel, enabled, limit));
    }

    @PostMapping("/sources")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertSource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertSource(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/keywords")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> keywords(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listKeywords(tenantIdOrDefault(tenantId), channel, status, limit));
    }

    @PostMapping("/keywords")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertKeyword(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertKeyword(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/snapshots")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> snapshots(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) Long keywordId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listSnapshots(tenantIdOrDefault(tenantId), channel, sourceId, keywordId,
                startDate, endDate, limit));
    }

    @PostMapping("/snapshots")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertSnapshot(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertSnapshot(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/opportunities")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> opportunities(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listOpportunities(tenantIdOrDefault(tenantId), channel, sourceId, status,
                severity, limit));
    }

    @PostMapping("/opportunities/evaluate")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> evaluateOpportunities(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.evaluateOpportunities(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/opportunities/{opportunityId}/status")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> updateOpportunityStatus(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long opportunityId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.updateOpportunityStatus(tenantIdOrDefault(tenantId), opportunityId,
                safePayload(payload), actorOrDefault(actor)));
    }

    @PostMapping("/opportunities/{opportunityId}/mutations")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createOpportunityMutation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long opportunityId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createOpportunityMutation(tenantIdOrDefault(tenantId), opportunityId,
                safePayload(payload), actorOrDefault(actor)));
    }

    @GetMapping("/mutations")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> mutations(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listMutations(tenantIdOrDefault(tenantId), sourceId, status, approvalStatus,
                limit));
    }

    @PostMapping("/mutations")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertMutation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertMutation(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/mutations/{mutationId}/approve")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> approveMutation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long mutationId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.approveMutation(tenantIdOrDefault(tenantId), mutationId, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/mutations/{mutationId}/execute")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> executeMutation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long mutationId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.executeMutation(tenantIdOrDefault(tenantId), mutationId, safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/url-inspections")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> urlInspections(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String indexedState,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listUrlInspections(tenantIdOrDefault(tenantId), sourceId, indexedState,
                startDate, endDate, limit));
    }

    @GetMapping("/sync-runs")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> syncRuns(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String runType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listSyncRuns(tenantIdOrDefault(tenantId), sourceId, runType, status, limit));
    }

    @PostMapping("/sources/{sourceId}/sync")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> syncSource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long sourceId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.syncSource(tenantIdOrDefault(tenantId), sourceId,
                withDefault(safePayload(payload), "runType", "PERFORMANCE"), actorOrDefault(actor)));
    }

    @PostMapping("/sources/sync-due")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> syncDue(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.syncDue(tenantIdOrDefault(tenantId), withDefault(safePayload(payload), "limit", 50),
                actorOrDefault(actor)));
    }

    @GetMapping("/provider-changes")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> providerChanges(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) Long mutationId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String reconciliationStatus,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listProviderChanges(tenantIdOrDefault(tenantId), sourceId, mutationId, provider,
                reconciliationStatus, limit));
    }

    @PostMapping("/mutations/{mutationId}/reconcile")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> reconcileMutation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long mutationId) {
        return envelope(() -> facade.reconcileMutation(tenantIdOrDefault(tenantId), mutationId, actorOrDefault(actor)));
    }

    @GetMapping("/impact-windows")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> impactWindows(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long opportunityId,
            @RequestParam(required = false) Long mutationId,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listImpactWindows(tenantIdOrDefault(tenantId), opportunityId, mutationId,
                sourceId, status, decision, limit));
    }

    @PostMapping("/impact-windows/evaluate-due")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> evaluateDueImpactWindows(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.evaluateDueImpactWindows(tenantIdOrDefault(tenantId),
                withDefault(safePayload(payload), "limit", 50), actorOrDefault(actor)));
    }

    @GetMapping("/readiness")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> readiness(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.readiness(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/summary")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> summary(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) Long keywordId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return envelope(() -> facade.summary(tenantIdOrDefault(tenantId), channel, sourceId, keywordId,
                startDate, endDate));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(supplier.get());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
        });
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CompatibilityEnvelope<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", status, message));
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    }

    private static Map<String, Object> withDefault(Map<String, Object> payload, String key, Object value) {
        payload.putIfAbsent(key, value);
        return payload;
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    public record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }
}
