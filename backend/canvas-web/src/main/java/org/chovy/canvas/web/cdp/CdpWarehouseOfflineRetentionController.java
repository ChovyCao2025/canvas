package org.chovy.canvas.web.cdp;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseOfflineRetentionFacade;
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
public class CdpWarehouseOfflineRetentionController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final CdpWarehouseOfflineRetentionFacade facade;

    public CdpWarehouseOfflineRetentionController(CdpWarehouseOfflineRetentionFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/offline-cycle/plan")
    public Mono<CompatibilityEnvelope<CdpWarehouseOfflineRetentionFacade.OfflineCyclePlanView>> offlineCyclePlan(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "1000") int backfillLimit,
            @RequestParam(defaultValue = "30") int aggregationWindowMinutes,
            @RequestParam(required = false) LocalDateTime now) {
        return envelope(() -> facade.offlineCyclePlan(tenantIdOrDefault(tenantId), now, backfillLimit,
                aggregationWindowMinutes));
    }

    @PostMapping("/offline-cycle/run")
    public Mono<CompatibilityEnvelope<CdpWarehouseOfflineRetentionFacade.OfflineCycleResultView>> offlineCycleRun(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> request = safePayload(payload);
        return envelope(() -> facade.runOfflineCycle(tenantIdOrDefault(tenantId), localDateTime(request.get("now")),
                intValue(request.get("backfillLimit"), 1000),
                intValue(request.get("aggregationWindowMinutes"), 30),
                stringValue(request.get("operator"))));
    }

    @GetMapping("/retention/plan")
    public Mono<CompatibilityEnvelope<CdpWarehouseOfflineRetentionFacade.RetentionPlanView>> retentionPlan(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "30") int syncRunRetentionDays,
            @RequestParam(defaultValue = "14") int realtimeRetryRetentionDays,
            @RequestParam(defaultValue = "90") int resolvedIncidentRetentionDays,
            @RequestParam(required = false) LocalDateTime now) {
        return envelope(() -> facade.retentionPlan(tenantIdOrDefault(tenantId), now, syncRunRetentionDays,
                realtimeRetryRetentionDays, resolvedIncidentRetentionDays));
    }

    @PostMapping("/retention/run")
    public Mono<CompatibilityEnvelope<CdpWarehouseOfflineRetentionFacade.RetentionCleanupResultView>> retentionRun(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> request = safePayload(payload);
        return envelope(() -> facade.runRetention(tenantIdOrDefault(tenantId), localDateTime(request.get("now")),
                intValue(request.get("syncRunRetentionDays"), 30),
                intValue(request.get("realtimeRetryRetentionDays"), 14),
                intValue(request.get("resolvedIncidentRetentionDays"), 90),
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
