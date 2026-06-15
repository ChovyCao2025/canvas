package org.chovy.canvas.web.cdp;

import java.util.List;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseSemanticMetricFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseSemanticMetricFacade.SemanticMetricView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/semantic-metrics")
public class CdpWarehouseSemanticMetricController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final CdpWarehouseSemanticMetricFacade facade;

    public CdpWarehouseSemanticMetricController(CdpWarehouseSemanticMetricFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<List<SemanticMetricView>>> listMetrics(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String datasetKey) {
        return envelope(() -> facade.listMetrics(tenantIdOrDefault(tenantId), datasetKey));
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
    }
}
