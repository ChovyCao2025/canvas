package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseMetricLineageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/metric-lineage")
public class CdpWarehouseMetricLineageController {

    private final CdpWarehouseMetricLineageService metricLineageService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseMetricLineageController(CdpWarehouseMetricLineageService metricLineageService) {
        this(metricLineageService, null);
    }

    @Autowired
    public CdpWarehouseMetricLineageController(CdpWarehouseMetricLineageService metricLineageService,
                                               TenantContextResolver tenantContextResolver) {
        this.metricLineageService = metricLineageService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/impact")
    public Mono<R<CdpWarehouseMetricLineageService.MetricImpactView>> impact(
            @RequestParam String datasetKey,
            @RequestParam String metricKey) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(metricLineageService.impact(tenantId, datasetKey, metricKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<Long> currentTenantId() {
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        return tenantContextResolver.current()
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .defaultIfEmpty(0L)
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }
}
