package org.chovy.canvas.web.cdp;

import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseTableFacade;
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
@RequestMapping("/warehouse/tables")
public class CdpWarehouseTableController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final CdpWarehouseTableFacade facade;

    public CdpWarehouseTableController(CdpWarehouseTableFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/contracts")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> contracts(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String layer,
            @RequestParam(required = false) String lifecycleStatus) {
        return envelope(() -> facade.listContracts(tenantIdOrDefault(tenantId), layer, lifecycleStatus));
    }

    @PostMapping("/contracts")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertContract(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.upsertContract(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/contracts/{tableKey}/inspect")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> inspectContract(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String tableKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.inspectContract(tenantIdOrDefault(tenantId), tableKey,
                operator(safePayload(payload), actor), false));
    }

    @PostMapping("/inspect")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> inspectAll(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.inspectAll(tenantIdOrDefault(tenantId), operator(safePayload(payload), actor),
                false));
    }

    @PostMapping("/contracts/{tableKey}/inspect-live")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> inspectLiveContract(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String tableKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.inspectContract(tenantIdOrDefault(tenantId), tableKey,
                operator(safePayload(payload), actor), true));
    }

    @PostMapping("/inspect-live")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> inspectLiveAll(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.inspectAll(tenantIdOrDefault(tenantId), operator(safePayload(payload), actor),
                true));
    }

    @PostMapping("/contracts/{tableKey}/remediation-plan")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> remediationPlan(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String tableKey,
            @RequestParam(defaultValue = "true") boolean live,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.planRemediation(tenantIdOrDefault(tenantId), tableKey, live,
                operator(safePayload(payload), actor)));
    }

    @PostMapping("/remediation-plan")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> remediationPlanAll(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestParam(defaultValue = "true") boolean live,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.planAllRemediation(tenantIdOrDefault(tenantId), live,
                operator(safePayload(payload), actor)));
    }

    @PostMapping("/incidents/scan")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> scanIncidents(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestParam(defaultValue = "true") boolean live,
            @RequestParam(required = false) String operator) {
        return envelope(() -> facade.scanIncidents(tenantIdOrDefault(tenantId), live,
                actorOrDefault(operator == null || operator.isBlank() ? actor : operator)));
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

    private static String operator(Map<String, Object> payload, String actor) {
        Object operator = payload.get("operator");
        if (operator != null && !String.valueOf(operator).isBlank()) {
            return String.valueOf(operator).trim();
        }
        return actorOrDefault(actor);
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
