package org.chovy.canvas.web.cdp;

import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseEnterpriseOlapEvidenceFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseEnterpriseOlapEvidenceFacade.EvidenceCommand;
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
@RequestMapping("/warehouse/enterprise-olap/evidence")
public class CdpWarehouseEnterpriseOlapEvidenceController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final CdpWarehouseEnterpriseOlapEvidenceFacade facade;

    public CdpWarehouseEnterpriseOlapEvidenceController(CdpWarehouseEnterpriseOlapEvidenceFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<Object>> record(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) EvidenceCommand command) {
        return envelope(() -> facade.record(tenantIdOrDefault(tenantId), command, "system"));
    }

    @GetMapping("/latest")
    public Mono<CompatibilityEnvelope<Object>> latest(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.latest(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/proof")
    public Mono<CompatibilityEnvelope<Object>> proof(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.proof(tenantIdOrDefault(tenantId)));
    }

    @PostMapping("/collect")
    public Mono<CompatibilityEnvelope<Object>> collect(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.collect(tenantIdOrDefault(tenantId), "MANUAL", "system"));
    }

    @GetMapping("/collections")
    public Mono<CompatibilityEnvelope<Object>> collections(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") Integer limit) {
        return envelope(() -> facade.collections(tenantIdOrDefault(tenantId), limit));
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

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
