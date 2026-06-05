package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.analytics.AudienceMaterializationOperationsService;
import org.chovy.canvas.domain.analytics.AudienceMaterializationScheduleService;
import org.chovy.canvas.domain.analytics.AudienceMaterializationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/warehouse/audiences")
public class CdpWarehouseAudienceMaterializationController {

    private final AudienceMaterializationOperationsService operationsService;
    private final AudienceMaterializationScheduleService scheduleService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseAudienceMaterializationController(
            AudienceMaterializationOperationsService operationsService) {
        this(operationsService, null, null);
    }

    public CdpWarehouseAudienceMaterializationController(
            AudienceMaterializationOperationsService operationsService,
            TenantContextResolver tenantContextResolver) {
        this(operationsService, tenantContextResolver, null);
    }

    @Autowired
    public CdpWarehouseAudienceMaterializationController(
            AudienceMaterializationOperationsService operationsService,
            TenantContextResolver tenantContextResolver,
            AudienceMaterializationScheduleService scheduleService) {
        this.operationsService = operationsService;
        this.scheduleService = scheduleService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/{audienceId}/materialize")
    public Mono<R<AudienceMaterializationService.MaterializationResult>> materialize(
            @PathVariable Long audienceId,
            @RequestBody(required = false) MaterializeReq req) {
        MaterializeReq request = req == null ? new MaterializeReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(operationsService.materialize(
                                normalizeTenant(context), audienceId, operator(request.getOperator(), context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{audienceId}/materialize-gated")
    public Mono<R<AudienceMaterializationOperationsService.GatedMaterializationResult>> materializeGated(
            @PathVariable Long audienceId,
            @RequestBody(required = false) GatedMaterializeReq req) {
        GatedMaterializeReq request = req == null ? new GatedMaterializeReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(operationsService.materializeWithAvailabilityGate(
                                normalizeTenant(context),
                                audienceId,
                                request.getFrom(),
                                request.getTo(),
                                request.getMode(),
                                request.isAllowWarn(),
                                operator(request.getOperator(), context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{audienceId}/materialize-contract-gated")
    public Mono<R<AudienceMaterializationOperationsService.ContractGatedMaterializationResult>> materializeContractGated(
            @PathVariable Long audienceId,
            @RequestBody(required = false) ContractGatedMaterializeReq req) {
        ContractGatedMaterializeReq request = req == null ? new ContractGatedMaterializeReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(operationsService.materializeWithConsumerAvailabilityContract(
                                normalizeTenant(context),
                                audienceId,
                                request.getContractKey(),
                                request.getFrom(),
                                request.getTo(),
                                operator(request.getOperator(), context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{audienceId}/materialization/rollback")
    public Mono<R<AudienceMaterializationOperationsService.RollbackView>> rollback(
            @PathVariable Long audienceId,
            @RequestBody RollbackReq req) {
        RollbackReq request = req == null ? new RollbackReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(operationsService.rollback(
                                normalizeTenant(context),
                                audienceId,
                                request.getTargetVersion(),
                                operator(request.getOperator(), context),
                                request.getReason())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/materialization/refresh-due")
    public Mono<R<AudienceMaterializationScheduleService.ScheduledRefreshResult>> refreshDue(
            @RequestBody(required = false) RefreshDueReq req) {
        RefreshDueReq request = req == null ? new RefreshDueReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (scheduleService == null) {
                        throw new IllegalStateException("audience materialization schedule service is not configured");
                    }
                    return R.ok(scheduleService.refreshDue(
                            normalizeTenant(context),
                            LocalDateTime.now(),
                            request.getLimit(),
                            operator(request.getOperator(), context)));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/materialization/refresh-due-gated")
    public Mono<R<AudienceMaterializationScheduleService.GatedScheduledRefreshResult>> refreshDueGated(
            @RequestBody(required = false) GatedRefreshDueReq req) {
        GatedRefreshDueReq request = req == null ? new GatedRefreshDueReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (scheduleService == null) {
                        throw new IllegalStateException("audience materialization schedule service is not configured");
                    }
                    LocalDateTime now = request.getNow() == null ? LocalDateTime.now() : request.getNow();
                    return R.ok(scheduleService.refreshDueWithAvailabilityGate(
                            normalizeTenant(context),
                            now,
                            request.getLimit(),
                            operator(request.getOperator(), context),
                            request.getFrom(),
                            request.getTo(),
                            request.getMode(),
                            request.isAllowWarn()));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/materialization-runs")
    public Mono<R<List<AudienceMaterializationOperationsService.RunView>>> recentRuns(
            @RequestParam(required = false) Long audienceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(operationsService.recentRuns(normalizeTenant(context), audienceId, status, limit)))
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

    private String operator(String requestedOperator, TenantContext context) {
        if (requestedOperator != null && !requestedOperator.isBlank()) {
            return requestedOperator.trim();
        }
        if (context != null && context.username() != null && !context.username().isBlank()) {
            return context.username();
        }
        return "operator";
    }

    @Data
    public static class MaterializeReq {
        private String operator;
    }

    @Data
    public static class GatedMaterializeReq {
        private LocalDateTime from;
        private LocalDateTime to;
        private String mode = "HYBRID";
        private boolean allowWarn;
        private String operator;
    }

    @Data
    public static class ContractGatedMaterializeReq {
        private String contractKey;
        private LocalDateTime from;
        private LocalDateTime to;
        private String operator;
    }

    @Data
    public static class RollbackReq {
        private Long targetVersion;
        private String operator;
        private String reason;
    }

    @Data
    public static class RefreshDueReq {
        private int limit;
        private String operator;
    }

    @Data
    public static class GatedRefreshDueReq {
        private LocalDateTime now;
        private LocalDateTime from;
        private LocalDateTime to;
        private String mode = "HYBRID";
        private boolean allowWarn;
        private int limit;
        private String operator;
    }
}
