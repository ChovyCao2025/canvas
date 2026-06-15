package org.chovy.canvas.web.cdp;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseOperationsFacade;
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
@RequestMapping("/warehouse")
public class CdpWarehouseOperationsController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final CdpWarehouseOperationsFacade facade;

    public CdpWarehouseOperationsController(CdpWarehouseOperationsFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/status")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> status(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.status(tenantIdOrDefault(tenantId), limit));
    }

    @PostMapping("/backfill")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> backfill(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> request = safePayload(payload);
        return envelope(() -> facade.triggerBackfill(tenantIdOrDefault(tenantId),
                longValue(request.get("lastId")), intValue(request.get("limit"), 0),
                stringValue(request.get("operator"))));
    }

    @PostMapping("/aggregate")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> aggregate(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> request = safePayload(payload);
        return envelope(() -> facade.triggerAggregation(tenantIdOrDefault(tenantId),
                localDateTime(request.get("from")), localDateTime(request.get("to")),
                stringValue(request.get("operator"))));
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

    private static LocalDateTime localDateTime(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return LocalDateTime.parse(String.valueOf(value));
    }

    private static Long longValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static int intValue(Object value, int defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
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
