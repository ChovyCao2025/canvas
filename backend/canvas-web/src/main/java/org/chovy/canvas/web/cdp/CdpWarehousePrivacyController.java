package org.chovy.canvas.web.cdp;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehousePrivacyFacade;
import org.springframework.http.HttpStatus;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/privacy")
public class CdpWarehousePrivacyController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final CdpWarehousePrivacyFacade facade;

    public CdpWarehousePrivacyController(CdpWarehousePrivacyFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/erasure/requests")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createErasureRequest(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createErasureRequest(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/erasure/requests/{requestId}/proofs")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> recordAssetProof(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long requestId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.recordAssetProof(tenantIdOrDefault(tenantId), requestId, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/erasure/requests/{requestId}/execute")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> executeErasure(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long requestId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.executeErasure(tenantIdOrDefault(tenantId), requestId, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/erasure/requests/{requestId}/audience-rebuild")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> rebuildAudienceBitmaps(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long requestId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.rebuildAudienceBitmaps(tenantIdOrDefault(tenantId), requestId,
                safePayload(payload), actorOrDefault(actor)));
    }

    @PostMapping("/erasure/audience-rebuild/automation/run")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> runAudienceRebuildAutomation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.runAudienceRebuildAutomation(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/erasure/audience-rebuild/automation/runs")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listAudienceRebuildAutomationRuns(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listAudienceRebuildAutomationRuns(tenantIdOrDefault(tenantId), limit));
    }

    @GetMapping("/erasure/audience-rebuild/automation/runs/{runId}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> getAudienceRebuildAutomationRun(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long runId) {
        return envelope(() -> facade.getAudienceRebuildAutomationRun(tenantIdOrDefault(tenantId), runId));
    }

    @GetMapping("/erasure/requests")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> recentErasureRequests(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.recentErasureRequests(tenantIdOrDefault(tenantId), status, limit));
    }

    @GetMapping("/erasure/requests/{requestId}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> getErasureRequest(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long requestId) {
        return envelope(() -> facade.getErasureRequest(tenantIdOrDefault(tenantId), requestId));
    }

    @GetMapping("/erasure/summary")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> erasureSummary(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.erasureSummary(tenantIdOrDefault(tenantId)));
    }

    @PostMapping("/tombstones")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createTombstone(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createTombstone(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/tombstones/from-erasure-request")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createTombstoneFromErasureRequest(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createTombstoneFromErasureRequest(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/tombstones/{tombstoneId}/revoke")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> revokeTombstone(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long tombstoneId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.revokeTombstone(tenantIdOrDefault(tenantId), tombstoneId, safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/tombstones")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listTombstones(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listTombstones(tenantIdOrDefault(tenantId), status, limit));
    }

    @GetMapping("/tombstones/decision")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> tombstoneDecision(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "USER_ID") String subjectType,
            @RequestParam String subjectValue) {
        return envelope(() -> facade.tombstoneDecision(tenantIdOrDefault(tenantId), subjectType, subjectValue));
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
        return payload == null ? Map.of() : payload;
    }

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
