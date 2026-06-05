package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/warehouse/fields")
public class CdpWarehouseFieldGovernanceController {

    private final CdpWarehouseFieldGovernanceService governanceService;
    private final TenantContextResolver tenantContextResolver;
    private final BiDatasetSpecResolver datasetSpecResolver;

    public CdpWarehouseFieldGovernanceController(CdpWarehouseFieldGovernanceService governanceService) {
        this(governanceService, null, BiDatasetSpecResolver.builtIn());
    }

    @Autowired
    public CdpWarehouseFieldGovernanceController(CdpWarehouseFieldGovernanceService governanceService,
                                                 TenantContextResolver tenantContextResolver,
                                                 ObjectProvider<BiDatasetSpecResolver> datasetSpecResolverProvider) {
        this(governanceService, tenantContextResolver,
                datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn));
    }

    public CdpWarehouseFieldGovernanceController(CdpWarehouseFieldGovernanceService governanceService,
                                                 TenantContextResolver tenantContextResolver) {
        this(governanceService, tenantContextResolver, BiDatasetSpecResolver.builtIn());
    }

    public CdpWarehouseFieldGovernanceController(CdpWarehouseFieldGovernanceService governanceService,
                                                 TenantContextResolver tenantContextResolver,
                                                 BiDatasetSpecResolver datasetSpecResolver) {
        this.governanceService = governanceService;
        this.tenantContextResolver = tenantContextResolver;
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
    }

    @GetMapping("/policies")
    public Mono<R<List<CdpWarehouseFieldGovernanceService.FieldPolicyView>>> listPolicies(
            @RequestParam(required = false) String datasetKey,
            @RequestParam(required = false) String lifecycleStatus) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(governanceService.listPolicies(normalizeTenant(context),
                                datasetKey,
                                lifecycleStatus)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/policies")
    public Mono<R<CdpWarehouseFieldGovernanceService.FieldPolicyView>> upsertPolicy(
            @RequestBody FieldPolicyReq req) {
        FieldPolicyReq request = req == null ? new FieldPolicyReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(governanceService.upsertPolicy(normalizeTenant(context), request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/evaluate-bi-query")
    public Mono<R<CdpWarehouseFieldGovernanceService.BiPolicyEvaluation>> evaluateBiQuery(
            @RequestBody BiQueryRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    Long tenantId = normalizeTenant(context);
                    var dataset = datasetSpecResolver.dataset(request.datasetKey(), tenantId);
                    return R.ok(governanceService.evaluateBiQuery(
                            dataset,
                            request,
                            new BiQueryContext(tenantId, context.username(), context.role()),
                            CdpWarehouseFieldGovernanceService.ACTION_BI_EVALUATE));
                })
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

    @Data
    public static class FieldPolicyReq {
        private String datasetKey;
        private String fieldKey;
        private String physicalName;
        private String columnName;
        private String valueType;
        private String semanticType;
        private String piiLevel;
        private String accessPolicy;
        private String minRole;
        private String allowedUsages;
        private String maskStrategy;
        private String lifecycleStatus;
        private String ownerName;
        private String description;

        CdpWarehouseFieldGovernanceService.FieldPolicyCommand toCommand() {
            return new CdpWarehouseFieldGovernanceService.FieldPolicyCommand(
                    datasetKey,
                    fieldKey,
                    physicalName,
                    columnName,
                    valueType,
                    semanticType,
                    piiLevel,
                    accessPolicy,
                    minRole,
                    allowedUsages,
                    maskStrategy,
                    lifecycleStatus,
                    ownerName,
                    description);
        }
    }
}
