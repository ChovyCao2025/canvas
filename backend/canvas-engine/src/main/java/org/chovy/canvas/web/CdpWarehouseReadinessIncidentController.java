package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseReadinessIncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/readiness/incidents")
public class CdpWarehouseReadinessIncidentController {

    private final CdpWarehouseReadinessIncidentService incidentService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseReadinessIncidentController(CdpWarehouseReadinessIncidentService incidentService) {
        this(incidentService, null);
    }

    @Autowired
    public CdpWarehouseReadinessIncidentController(CdpWarehouseReadinessIncidentService incidentService,
                                                   TenantContextResolver tenantContextResolver) {
        this.incidentService = incidentService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/scan")
    public Mono<R<CdpWarehouseReadinessIncidentService.ScanResult>> scan() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(incidentService.scan(tenantId)))
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
