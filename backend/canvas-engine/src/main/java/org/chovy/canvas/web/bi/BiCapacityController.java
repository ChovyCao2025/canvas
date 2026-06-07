package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityAlertPolicyCommand;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityAlertPolicyView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityService;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacitySummaryView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueService;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueSnapshotView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineTenantPoolPolicyCommand;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineTenantPoolPolicyView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/bi/capacity")
public class BiCapacityController {

    private final TenantContextResolver tenantContextResolver;
    private final BiQuickEngineCapacityService quickEngineCapacityService;
    private final BiQuickEngineQueueService quickEngineQueueService;

    @Autowired
    public BiCapacityController(TenantContextResolver tenantContextResolver,
                                BiQuickEngineCapacityService quickEngineCapacityService,
                                BiQuickEngineQueueService quickEngineQueueService) {
        this.tenantContextResolver = tenantContextResolver;
        this.quickEngineCapacityService = quickEngineCapacityService;
        this.quickEngineQueueService = quickEngineQueueService;
    }

    public BiCapacityController(TenantContextResolver tenantContextResolver,
                                BiQuickEngineCapacityService quickEngineCapacityService) {
        this(tenantContextResolver, quickEngineCapacityService, null);
    }

    @GetMapping("/quick-engine")
    public Mono<R<BiQuickEngineCapacitySummaryView>> quickEngineCapacity(
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(quickEngineCapacityService.summary(context.tenantId(), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/quick-engine/queue")
    public Mono<R<BiQuickEngineQueueSnapshotView>> quickEngineQueue(
            @RequestParam(required = false) String poolKey,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(quickEngineQueueService.snapshot(context.tenantId(), poolKey, status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/quick-engine/alert-policy")
    public Mono<R<BiQuickEngineCapacityAlertPolicyView>> upsertQuickEngineCapacityAlertPolicy(
            @RequestBody BiQuickEngineCapacityAlertPolicyCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(quickEngineCapacityService.upsertAlertPolicy(
                                context.tenantId(),
                                command,
                                context.username())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/quick-engine/tenant-pool-policy")
    public Mono<R<BiQuickEngineTenantPoolPolicyView>> upsertQuickEngineTenantPoolPolicy(
            @RequestBody BiQuickEngineTenantPoolPolicyCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(quickEngineCapacityService.upsertTenantPoolPolicy(
                                context.tenantId(),
                                command,
                                context.username())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }
}
