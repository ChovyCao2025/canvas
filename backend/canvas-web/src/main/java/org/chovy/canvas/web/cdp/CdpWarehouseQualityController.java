package org.chovy.canvas.web.cdp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseQualityFacade;
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
@RequestMapping("/warehouse/quality")
public class CdpWarehouseQualityController {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final String DEFAULT_OPERATOR = "operator";

    private final CdpWarehouseQualityFacade facade;

    public CdpWarehouseQualityController(CdpWarehouseQualityFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/checks")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listChecks(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.recentChecks(tenantIdOrDefault(tenantId), limit));
    }

    @PostMapping("/reconcile-ods")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> reconcileOds(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> request = safePayload(payload);
        return envelope(() -> facade.reconcileOds(tenantIdOrDefault(tenantId), localDateTime(request.get("from")),
                localDateTime(request.get("to")), longValue(request.get("tolerance"), 0L), operator(request)));
    }

    @PostMapping("/aggregate-lag")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> aggregateLag(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> request = safePayload(payload);
        return envelope(() -> facade.checkAggregateLag(tenantIdOrDefault(tenantId), localDateTime(request.get("now")),
                longValue(request.get("maxLagMinutes"), 30L), operator(request)));
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

    private static String operator(Map<String, Object> payload) {
        Object operator = payload.get("operator");
        return operator == null || String.valueOf(operator).isBlank()
                ? DEFAULT_OPERATOR
                : String.valueOf(operator).trim();
    }

    private static Long longValue(Object value, long defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
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

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
