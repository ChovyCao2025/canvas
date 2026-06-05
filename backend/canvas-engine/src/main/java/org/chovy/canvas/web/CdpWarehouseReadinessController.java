package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseReadinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse")
public class CdpWarehouseReadinessController {

    private final CdpWarehouseReadinessService readinessService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseReadinessController(CdpWarehouseReadinessService readinessService) {
        this(readinessService, null);
    }

    @Autowired
    public CdpWarehouseReadinessController(CdpWarehouseReadinessService readinessService,
                                           TenantContextResolver tenantContextResolver) {
        this.readinessService = readinessService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/readiness")
    public Mono<R<CdpWarehouseReadinessService.ReadinessSummary>> readiness() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(readinessService.readiness(tenantId)))
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
