package org.chovy.canvas.web.cdp;

import java.util.List;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseE2eCertificationFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/e2e-certification-runs")
public class CdpWarehouseE2eCertificationRunController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final CdpWarehouseE2eCertificationFacade facade;

    public CdpWarehouseE2eCertificationRunController(CdpWarehouseE2eCertificationFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<Object>> run(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "HYBRID") String mode,
            @RequestParam(name = "contractKey", required = false) List<String> contractKeys,
            @RequestParam(defaultValue = "true") boolean requirePhysical,
            @RequestParam(defaultValue = "true") boolean requireRealtime,
            @RequestParam(defaultValue = "true") boolean requireDataPathProof,
            @RequestParam(defaultValue = "system") String requestedBy) {
        return envelope(() -> facade.run(tenantIdOrDefault(tenantId), from, to, mode, safeContractKeys(contractKeys),
                requirePhysical, requireRealtime, requireDataPathProof, requestedBy));
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<Object>> recent(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") Integer limit) {
        return envelope(() -> facade.recent(tenantIdOrDefault(tenantId), limit));
    }

    @GetMapping("/{id}")
    public Mono<CompatibilityEnvelope<Object>> get(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> facade.get(tenantIdOrDefault(tenantId), id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private static Mono<CompatibilityEnvelope<Object>> envelope(Supplier<Object> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static List<String> safeContractKeys(List<String> contractKeys) {
        return contractKeys == null ? List.of() : contractKeys;
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
