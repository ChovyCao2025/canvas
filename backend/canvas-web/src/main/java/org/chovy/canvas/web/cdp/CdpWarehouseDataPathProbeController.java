package org.chovy.canvas.web.cdp;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseDataPathProbeFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/data-path-probes/synthetic-ods")
public class CdpWarehouseDataPathProbeController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final CdpWarehouseDataPathProbeFacade facade;

    public CdpWarehouseDataPathProbeController(CdpWarehouseDataPathProbeFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/run")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> run(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String probeKey,
            @RequestParam(required = false) String eventCode,
            @RequestParam(defaultValue = "true") boolean strict,
            @RequestParam(defaultValue = "3") int verifyAttempts,
            @RequestParam(defaultValue = "100") int verifyDelayMs,
            @RequestParam(defaultValue = "DIRECT_SINK") String sourceMode) {
        return envelope(() -> facade.run(tenantIdOrDefault(tenantId),
                new CdpWarehouseDataPathProbeFacade.RunCommand(
                        probeKey, eventCode, strict, verifyAttempts, verifyDelayMs, sourceMode)));
    }

    @GetMapping("/runs")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> recent(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.recent(tenantIdOrDefault(tenantId), limit));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
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
