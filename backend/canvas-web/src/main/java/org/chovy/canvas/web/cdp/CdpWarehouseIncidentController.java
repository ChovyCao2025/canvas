package org.chovy.canvas.web.cdp;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseIncidentFacade;
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
@RequestMapping("/warehouse/incidents")
public class CdpWarehouseIncidentController {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final String DEFAULT_OPERATOR = "operator";

    private final CdpWarehouseIncidentFacade facade;

    public CdpWarehouseIncidentController(CdpWarehouseIncidentFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> list(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.listIncidents(tenantIdOrDefault(tenantId), status, limit));
    }

    @PostMapping("/{id}/ack")
    public Mono<CompatibilityEnvelope<Boolean>> acknowledge(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.acknowledge(tenantIdOrDefault(tenantId), id, operator(payload)));
    }

    @PostMapping("/{id}/resolve")
    public Mono<CompatibilityEnvelope<Boolean>> resolve(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.resolve(tenantIdOrDefault(tenantId), id, operator(payload)));
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

    private static String operator(Map<String, Object> payload) {
        if (payload == null) {
            return DEFAULT_OPERATOR;
        }
        Object operator = payload.get("operator");
        return operator == null || String.valueOf(operator).isBlank()
                ? DEFAULT_OPERATOR
                : String.valueOf(operator).trim();
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
