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
/**
 * CdpWarehouseFieldGovernanceController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class CdpWarehouseFieldGovernanceController {

    private final CdpWarehouseFieldGovernanceService governanceService;
    private final TenantContextResolver tenantContextResolver;
    private final BiDatasetSpecResolver datasetSpecResolver;

    /**
     * 初始化 CdpWarehouseFieldGovernanceController 实例。
     *
     * @param governanceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseFieldGovernanceController(CdpWarehouseFieldGovernanceService governanceService) {
        this(governanceService, null, BiDatasetSpecResolver.builtIn());
    }

    @Autowired
    /**
     * 初始化 CdpWarehouseFieldGovernanceController 实例。
     *
     * @param governanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetSpecResolverProvider 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehouseFieldGovernanceController(CdpWarehouseFieldGovernanceService governanceService,
                                                 TenantContextResolver tenantContextResolver,
                                                 ObjectProvider<BiDatasetSpecResolver> datasetSpecResolverProvider) {
        this(governanceService, tenantContextResolver,
                datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn));
    }

    /**
     * 初始化 CdpWarehouseFieldGovernanceController 实例。
     *
     * @param governanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehouseFieldGovernanceController(CdpWarehouseFieldGovernanceService governanceService,
                                                 TenantContextResolver tenantContextResolver) {
        this(governanceService, tenantContextResolver, BiDatasetSpecResolver.builtIn());
    }

    /**
     * 初始化 CdpWarehouseFieldGovernanceController 实例。
     *
     * @param governanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehouseFieldGovernanceController(CdpWarehouseFieldGovernanceService governanceService,
                                                 TenantContextResolver tenantContextResolver,
                                                 BiDatasetSpecResolver datasetSpecResolver) {
        this.governanceService = governanceService;
        this.tenantContextResolver = tenantContextResolver;
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
    }

    @GetMapping("/policies")
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param lifecycleStatus 业务状态，用于筛选或推进状态流转。
     * @return 返回符合条件的数据列表或视图。
     */
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
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<CdpWarehouseFieldGovernanceService.FieldPolicyView>> upsertPolicy(
            @RequestBody FieldPolicyReq req) {
        FieldPolicyReq request = req == null ? new FieldPolicyReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(governanceService.upsertPolicy(normalizeTenant(context), request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/evaluate-bi-query")
    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 evaluateBiQuery 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    @Data
    /**
     * FieldPolicyReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
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
