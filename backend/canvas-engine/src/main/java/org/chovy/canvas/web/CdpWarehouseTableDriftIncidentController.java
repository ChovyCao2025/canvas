package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseTableDriftIncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/tables/incidents")
public class CdpWarehouseTableDriftIncidentController {

    private final CdpWarehouseTableDriftIncidentService incidentService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseTableDriftIncidentController(CdpWarehouseTableDriftIncidentService incidentService) {
        this(incidentService, null);
    }

    @Autowired
    public CdpWarehouseTableDriftIncidentController(
            CdpWarehouseTableDriftIncidentService incidentService,
            TenantContextResolver tenantContextResolver) {
        this.incidentService = incidentService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/scan")
    public Mono<R<CdpWarehouseTableDriftIncidentService.ScanResult>> scan(
            @RequestParam(defaultValue = "true") boolean live,
            @RequestParam(required = false) String operator) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(incidentService.scan(tenantId, live, operator)))
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
