package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAggregationService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseBackfillService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseOperationsService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRetentionService;
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

@RestController
@RequestMapping("/warehouse")
public class CdpWarehouseController {

    private final CdpWarehouseOperationsService operationsService;
    private final CdpWarehouseRetentionService retentionService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseController(CdpWarehouseOperationsService operationsService) {
        this(operationsService, null, null);
    }

    public CdpWarehouseController(CdpWarehouseOperationsService operationsService,
                                  TenantContextResolver tenantContextResolver) {
        this(operationsService, null, tenantContextResolver);
    }

    @Autowired
    public CdpWarehouseController(CdpWarehouseOperationsService operationsService,
                                  CdpWarehouseRetentionService retentionService,
                                  TenantContextResolver tenantContextResolver) {
        this.operationsService = operationsService;
        this.retentionService = retentionService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/status")
    public Mono<R<CdpWarehouseOperationsService.WarehouseStatus>> status(
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(operationsService.status(tenantId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/backfill")
    public Mono<R<CdpWarehouseBackfillService.BackfillResult>> backfill(@RequestBody BackfillReq req) {
        BackfillReq request = req == null ? new BackfillReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(operationsService.triggerBackfill(
                                tenantId,
                                request.getLastId(),
                                request.getLimit(),
                                request.getOperator())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/aggregate")
    public Mono<R<CdpWarehouseAggregationService.AggregationResult>> aggregate(@RequestBody AggregateReq req) {
        AggregateReq request = req == null ? new AggregateReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(operationsService.triggerAggregation(
                                tenantId,
                                request.getFrom(),
                                request.getTo(),
                                request.getOperator())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/offline-cycle/plan")
    public Mono<R<CdpWarehouseOperationsService.OfflineCyclePlan>> offlineCyclePlan(
            @RequestParam(defaultValue = "1000") int backfillLimit,
            @RequestParam(defaultValue = "30") int aggregationWindowMinutes,
            @RequestParam(required = false) LocalDateTime now) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(operationsService.planOfflineCycle(
                                tenantId, now, backfillLimit, aggregationWindowMinutes)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/offline-cycle/run")
    public Mono<R<CdpWarehouseOperationsService.OfflineCycleResult>> offlineCycleRun(
            @RequestBody OfflineCycleReq req) {
        OfflineCycleReq request = req == null ? new OfflineCycleReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(operationsService.runOfflineCycle(
                                tenantId,
                                request.getNow(),
                                request.getBackfillLimit(),
                                request.getAggregationWindowMinutes(),
                                request.getOperator())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/retention/plan")
    public Mono<R<CdpWarehouseRetentionService.RetentionPlan>> retentionPlan(
            @RequestParam(defaultValue = "30") int syncRunRetentionDays,
            @RequestParam(defaultValue = "14") int realtimeRetryRetentionDays,
            @RequestParam(defaultValue = "90") int resolvedIncidentRetentionDays,
            @RequestParam(required = false) LocalDateTime now) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(requireRetentionService().plan(
                                tenantId,
                                now,
                                syncRunRetentionDays,
                                realtimeRetryRetentionDays,
                                resolvedIncidentRetentionDays)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/retention/run")
    public Mono<R<CdpWarehouseRetentionService.RetentionCleanupResult>> retentionRun(
            @RequestBody RetentionReq req) {
        RetentionReq request = req == null ? new RetentionReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(requireRetentionService().cleanup(
                                tenantId,
                                request.getNow(),
                                request.getSyncRunRetentionDays(),
                                request.getRealtimeRetryRetentionDays(),
                                request.getResolvedIncidentRetentionDays(),
                                request.getOperator())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private CdpWarehouseRetentionService requireRetentionService() {
        if (retentionService == null) {
            throw new IllegalStateException("warehouse retention service is not configured");
        }
        return retentionService;
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
    public static class BackfillReq {
        private Long lastId;
        private int limit = 1000;
        private String operator;
    }

    @Data
    public static class AggregateReq {
        private LocalDateTime from;
        private LocalDateTime to;
        private String operator;
    }

    @Data
    public static class OfflineCycleReq {
        private LocalDateTime now;
        private int backfillLimit = 1000;
        private int aggregationWindowMinutes = 30;
        private String operator;
    }

    @Data
    public static class RetentionReq {
        private LocalDateTime now;
        private int syncRunRetentionDays = 30;
        private int realtimeRetryRetentionDays = 14;
        private int resolvedIncidentRetentionDays = 90;
        private String operator;
    }
}
