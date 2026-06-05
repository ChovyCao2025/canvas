package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeCheckpointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/realtime")
public class CdpWarehouseRealtimeController {

    private final CdpWarehouseRealtimeCheckpointService checkpointService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseRealtimeController(CdpWarehouseRealtimeCheckpointService checkpointService) {
        this(checkpointService, null);
    }

    @Autowired
    public CdpWarehouseRealtimeController(CdpWarehouseRealtimeCheckpointService checkpointService,
                                          TenantContextResolver tenantContextResolver) {
        this.checkpointService = checkpointService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/status")
    public Mono<R<CdpWarehouseRealtimeCheckpointService.RealtimeStatus>> status() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(checkpointService.status(tenantId)))
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
