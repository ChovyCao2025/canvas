package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseSyntheticDataPathProbeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/warehouse/data-path-probes/synthetic-ods")
public class CdpWarehouseSyntheticDataPathProbeController {

    private final CdpWarehouseSyntheticDataPathProbeService probeService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseSyntheticDataPathProbeController(
            CdpWarehouseSyntheticDataPathProbeService probeService) {
        this(probeService, null);
    }

    @Autowired
    public CdpWarehouseSyntheticDataPathProbeController(
            CdpWarehouseSyntheticDataPathProbeService probeService,
            TenantContextResolver tenantContextResolver) {
        this.probeService = probeService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/run")
    public Mono<R<CdpWarehouseSyntheticDataPathProbeService.ProbeRunView>> run(
            @RequestParam(required = false) String probeKey,
            @RequestParam(required = false) String eventCode,
            @RequestParam(defaultValue = "true") boolean strict,
            @RequestParam(defaultValue = "3") int verifyAttempts,
            @RequestParam(defaultValue = "100") int verifyDelayMs,
            @RequestParam(defaultValue = "DIRECT_SINK") String sourceMode) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.run(
                                normalizeTenant(context),
                                new CdpWarehouseSyntheticDataPathProbeService.RunCommand(
                                        probeKey, eventCode, strict, verifyAttempts, verifyDelayMs, sourceMode))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/runs")
    public Mono<R<List<CdpWarehouseSyntheticDataPathProbeService.ProbeRunView>>> recent(
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.recent(normalizeTenant(context), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    private Long normalizeTenant(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }
}
