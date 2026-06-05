package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseTableGovernanceService;
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

import java.util.List;

@RestController
@RequestMapping("/warehouse/tables")
public class CdpWarehouseTableGovernanceController {

    private final CdpWarehouseTableGovernanceService governanceService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseTableGovernanceController(CdpWarehouseTableGovernanceService governanceService) {
        this(governanceService, null);
    }

    @Autowired
    public CdpWarehouseTableGovernanceController(CdpWarehouseTableGovernanceService governanceService,
                                                 TenantContextResolver tenantContextResolver) {
        this.governanceService = governanceService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/contracts")
    public Mono<R<List<CdpWarehouseTableGovernanceService.TableContractView>>> listContracts(
            @RequestParam(required = false) String layer,
            @RequestParam(required = false) String lifecycleStatus) {
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.listContracts(
                                normalizeTenant(context), layer, lifecycleStatus)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/contracts")
    public Mono<R<CdpWarehouseTableGovernanceService.TableContractView>> upsertContract(
            @RequestBody TableContractReq req) {
        TableContractReq request = req == null ? new TableContractReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.upsertContract(
                                normalizeTenant(context), request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/contracts/{tableKey}/inspect")
    public Mono<R<CdpWarehouseTableGovernanceService.InspectionReport>> inspectContract(
            @PathVariable String tableKey,
            @RequestBody(required = false) InspectionReq req) {
        InspectionReq request = req == null ? new InspectionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.inspectContract(
                                normalizeTenant(context), tableKey, operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/inspect")
    public Mono<R<CdpWarehouseTableGovernanceService.InspectionSummary>> inspectAll(
            @RequestBody(required = false) InspectionReq req) {
        InspectionReq request = req == null ? new InspectionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.inspectAll(
                                normalizeTenant(context), operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/contracts/{tableKey}/inspect-live")
    public Mono<R<CdpWarehouseTableGovernanceService.InspectionReport>> inspectLiveContract(
            @PathVariable String tableKey,
            @RequestBody(required = false) InspectionReq req) {
        InspectionReq request = req == null ? new InspectionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.inspectLiveContract(
                                normalizeTenant(context), tableKey, operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/inspect-live")
    public Mono<R<CdpWarehouseTableGovernanceService.InspectionSummary>> inspectLiveAll(
            @RequestBody(required = false) InspectionReq req) {
        InspectionReq request = req == null ? new InspectionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.inspectLiveAll(
                                normalizeTenant(context), operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/contracts/{tableKey}/remediation-plan")
    public Mono<R<CdpWarehouseTableGovernanceService.TableRemediationPlan>> remediationPlan(
            @PathVariable String tableKey,
            @RequestParam(defaultValue = "true") boolean live,
            @RequestBody(required = false) InspectionReq req) {
        InspectionReq request = req == null ? new InspectionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.planRemediation(
                                normalizeTenant(context), tableKey, live, operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/remediation-plan")
    public Mono<R<CdpWarehouseTableGovernanceService.RemediationSummary>> remediationPlanAll(
            @RequestParam(defaultValue = "true") boolean live,
            @RequestBody(required = false) InspectionReq req) {
        InspectionReq request = req == null ? new InspectionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(governanceService.planAllRemediation(
                                normalizeTenant(context), live, operator(request, context))))
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

    private String operator(InspectionReq request, TenantContext context) {
        if (request != null && request.getOperator() != null && !request.getOperator().isBlank()) {
            return request.getOperator().trim();
        }
        if (context != null && context.username() != null && !context.username().isBlank()) {
            return context.username();
        }
        return "operator";
    }

    @Data
    public static class TableContractReq {
        private String tableKey;
        private String datasetKey;
        private String layer;
        private String physicalName;
        private String engineType;
        private String ddlAssetPath;
        private String partitionColumn;
        private String partitionGranularity;
        private Integer retentionDays;
        private Integer replicaCount;
        private Integer bucketCount;
        private String distributionColumns;
        private String storagePolicy;
        private String lifecycleStatus;
        private String ownerName;
        private String description;
        private String expectedPropertiesJson;

        CdpWarehouseTableGovernanceService.TableContractCommand toCommand() {
            return new CdpWarehouseTableGovernanceService.TableContractCommand(
                    tableKey,
                    datasetKey,
                    layer,
                    physicalName,
                    engineType,
                    ddlAssetPath,
                    partitionColumn,
                    partitionGranularity,
                    retentionDays,
                    replicaCount,
                    bucketCount,
                    distributionColumns,
                    storagePolicy,
                    lifecycleStatus,
                    ownerName,
                    description,
                    expectedPropertiesJson);
        }
    }

    @Data
    public static class InspectionReq {
        private String operator;
    }
}
