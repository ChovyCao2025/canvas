package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeJobIncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/realtime/jobs/incidents")
public class CdpWarehouseRealtimeJobIncidentController {

    private final CdpWarehouseRealtimeJobIncidentService incidentService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseRealtimeJobIncidentController(
            CdpWarehouseRealtimeJobIncidentService incidentService) {
        this(incidentService, null);
    }

    @Autowired
    public CdpWarehouseRealtimeJobIncidentController(
            CdpWarehouseRealtimeJobIncidentService incidentService,
            TenantContextResolver tenantContextResolver) {
        this.incidentService = incidentService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/scan")
    public Mono<R<CdpWarehouseRealtimeJobIncidentService.ScanResult>> scan(
            @RequestParam(required = false) String pipelineKey,
            @RequestParam(defaultValue = "300") long maxHeartbeatAgeSeconds,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(incidentService.scan(
                                tenantId, pipelineKey, maxHeartbeatAgeSeconds, limit)))
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
