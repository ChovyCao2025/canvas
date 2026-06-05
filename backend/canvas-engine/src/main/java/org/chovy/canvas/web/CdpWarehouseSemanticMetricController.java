package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseSemanticMetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/warehouse/semantic-metrics")
public class CdpWarehouseSemanticMetricController {

    private final CdpWarehouseSemanticMetricService metricService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseSemanticMetricController(CdpWarehouseSemanticMetricService metricService) {
        this(metricService, null);
    }

    @Autowired
    public CdpWarehouseSemanticMetricController(CdpWarehouseSemanticMetricService metricService,
                                                TenantContextResolver tenantContextResolver) {
        this.metricService = metricService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public Mono<R<List<CdpWarehouseSemanticMetricService.SemanticMetricView>>> listMetrics(
            @RequestParam(required = false) String datasetKey) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(metricService.listMetrics(tenantId, datasetKey)))
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
