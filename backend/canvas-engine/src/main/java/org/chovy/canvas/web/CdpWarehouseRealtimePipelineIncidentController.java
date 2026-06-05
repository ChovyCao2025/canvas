package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimePipelineIncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/realtime/pipelines/incidents")
public class CdpWarehouseRealtimePipelineIncidentController {

    private final CdpWarehouseRealtimePipelineIncidentService incidentService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseRealtimePipelineIncidentController(
            CdpWarehouseRealtimePipelineIncidentService incidentService) {
        this(incidentService, null);
    }

    @Autowired
    public CdpWarehouseRealtimePipelineIncidentController(
            CdpWarehouseRealtimePipelineIncidentService incidentService,
            TenantContextResolver tenantContextResolver) {
        this.incidentService = incidentService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/scan")
    public Mono<R<CdpWarehouseRealtimePipelineIncidentService.ScanResult>> scan(
            @RequestParam(defaultValue = "5") int recentLimit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(incidentService.scan(tenantId, recentLimit)))
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
