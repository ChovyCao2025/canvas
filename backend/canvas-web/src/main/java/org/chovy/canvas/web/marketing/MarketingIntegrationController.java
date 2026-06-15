package org.chovy.canvas.web.marketing;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.marketing.api.MarketingIntegrationFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/canvas/marketing-integrations")
public class MarketingIntegrationController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final MarketingIntegrationFacade facade;

    public MarketingIntegrationController(MarketingIntegrationFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/contracts")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertContract(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertContract(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/contracts")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listContracts(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String providerFamily,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listContracts(tenantIdOrDefault(tenantId), status, providerFamily, limit));
    }

    @GetMapping("/contracts/{contractId}/audit-events")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listContractAuditEvents(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long contractId,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listContractAuditEvents(tenantIdOrDefault(tenantId), contractId, limit));
    }

    @DeleteMapping("/contracts/{contractId}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> archiveContract(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long contractId) {
        return envelope(() -> facade.archiveContract(tenantIdOrDefault(tenantId), contractId, actorOrDefault(actor)));
    }

    @PostMapping("/contracts/{contractId}/probe-runs")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> recordProbeRun(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long contractId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.recordProbeRun(tenantIdOrDefault(tenantId), contractId, safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/contract-probe-runs")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listProbeRuns(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String providerFamily,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listProbeRuns(tenantIdOrDefault(tenantId), status, providerFamily, limit));
    }

    @PostMapping("/contract-probe-runs/scan")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> scanProbeRuns(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.scanProbeRuns(tenantIdOrDefault(tenantId), limit, actorOrDefault(actor)));
    }

    @GetMapping("/contract-slo-evaluations")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listContractSloEvaluations(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listContractSloEvaluations(tenantIdOrDefault(tenantId), limit));
    }

    @PostMapping("/contracts/{contractId}/probes")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> recordProbe(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long contractId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.recordProbe(tenantIdOrDefault(tenantId), contractId, safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/contracts/{contractId}/probes")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listContractProbes(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long contractId,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listContractProbes(tenantIdOrDefault(tenantId), contractId, limit));
    }

    @GetMapping("/probes")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listRecentProbes(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listRecentProbes(tenantIdOrDefault(tenantId), status, limit));
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
