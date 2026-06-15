package org.chovy.canvas.web.cdp;

import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseMetricLineageFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseMetricLineageFacade.MetricImpactView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/metric-lineage")
public class CdpWarehouseMetricLineageController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final CdpWarehouseMetricLineageFacade facade;

    public CdpWarehouseMetricLineageController(CdpWarehouseMetricLineageFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/impact")
    public Mono<CompatibilityEnvelope<MetricImpactView>> impact(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String datasetKey,
            @RequestParam String metricKey) {
        return envelope(() -> facade.impact(tenantIdOrDefault(tenantId), datasetKey, metricKey));
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

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
