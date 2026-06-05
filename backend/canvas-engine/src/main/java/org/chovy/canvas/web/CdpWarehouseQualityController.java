package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseQualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/warehouse/quality")
public class CdpWarehouseQualityController {

    private final CdpWarehouseQualityService qualityService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseQualityController(CdpWarehouseQualityService qualityService) {
        this(qualityService, null);
    }

    @Autowired
    public CdpWarehouseQualityController(CdpWarehouseQualityService qualityService,
                                         TenantContextResolver tenantContextResolver) {
        this.qualityService = qualityService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/checks")
    public Mono<R<List<CdpWarehouseQualityService.QualityCheckResult>>> listChecks(
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(qualityService.recentChecks(tenantId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/reconcile-ods")
    public Mono<R<CdpWarehouseQualityService.QualityCheckResult>> reconcileOds(
            @RequestBody ReconcileOdsReq req) {
        ReconcileOdsReq request = req == null ? new ReconcileOdsReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(qualityService.reconcileOds(
                                tenantId,
                                request.getFrom(),
                                request.getTo(),
                                request.getTolerance(),
                                request.getOperator())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/aggregate-lag")
    public Mono<R<CdpWarehouseQualityService.QualityCheckResult>> aggregateLag(
            @RequestBody AggregateLagReq req) {
        AggregateLagReq request = req == null ? new AggregateLagReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(qualityService.checkAggregateLag(
                                tenantId,
                                request.getNow(),
                                request.getMaxLagMinutes(),
                                request.getOperator())))
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

    @Data
    public static class ReconcileOdsReq {
        private LocalDateTime from;
        private LocalDateTime to;
        private long tolerance;
        private String operator;
    }

    @Data
    public static class AggregateLagReq {
        private LocalDateTime now;
        private long maxLagMinutes = 30;
        private String operator;
    }
}
