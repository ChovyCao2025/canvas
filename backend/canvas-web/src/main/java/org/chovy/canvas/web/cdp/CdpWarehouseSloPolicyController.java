package org.chovy.canvas.web.cdp;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseSloPolicyFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseSloPolicyCatalog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/slo-policies")
public class CdpWarehouseSloPolicyController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final CdpWarehouseSloPolicyFacade facade;

    public CdpWarehouseSloPolicyController(CdpWarehouseSloPolicyFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listPolicies(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String status) {
        return envelope(() -> facade.listPolicies(tenantIdOrDefault(tenantId), status));
    }

    @GetMapping("/effective")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> effective(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = CdpWarehouseSloPolicyCatalog.DEFAULT_POLICY_KEY) String policyKey) {
        return envelope(() -> facade.effectivePolicy(tenantIdOrDefault(tenantId), policyKey));
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsert(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.upsertPolicy(tenantIdOrDefault(tenantId), safePayload(payload)));
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
