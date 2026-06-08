package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardPreset;
import org.chovy.canvas.domain.bi.dashboard.MarketingBiDashboardPresetRegistry;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicket;
import org.chovy.canvas.domain.bi.embed.BiEmbedTokenCleanupResult;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketPayload;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketRequest;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketService;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketVerifyRequest;
import org.chovy.canvas.domain.bi.portal.BiPortalMenuResource;
import org.chovy.canvas.domain.bi.portal.BiPortalResource;
import org.chovy.canvas.domain.bi.portal.BiPortalRuntimeService;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiCompiledQuery;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealth;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealthProvider;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealthSloSummary;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealthSnapshot;
import org.chovy.canvas.domain.bi.query.BiFieldSpec;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryCompiler;
import org.chovy.canvas.domain.bi.query.BiQueryCancellationResult;
import org.chovy.canvas.domain.bi.query.BiQueryCacheInvalidationCommand;
import org.chovy.canvas.domain.bi.query.BiQueryCacheInvalidationResult;
import org.chovy.canvas.domain.bi.query.BiQueryCacheStats;
import org.chovy.canvas.domain.bi.query.BiQueryCachePolicyService;
import org.chovy.canvas.domain.bi.query.BiQueryCachePolicyUpdateCommand;
import org.chovy.canvas.domain.bi.query.BiQueryCachePolicyView;
import org.chovy.canvas.domain.bi.query.BiQueryExecutionService;
import org.chovy.canvas.domain.bi.query.BiQueryExplanation;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryDetail;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryItem;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryReader;
import org.chovy.canvas.domain.bi.query.BiQueryGovernanceAuditEntry;
import org.chovy.canvas.domain.bi.query.BiQueryGovernancePolicy;
import org.chovy.canvas.domain.bi.query.BiQueryGovernancePolicyService;
import org.chovy.canvas.domain.bi.query.BiQueryGovernancePolicyUpdateCommand;
import org.chovy.canvas.domain.bi.query.BiQueryGovernancePolicyView;
import org.chovy.canvas.domain.bi.query.BiQueryGovernanceSummary;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.chovy.canvas.domain.bi.query.BiQueryResultCache;
import org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * BiQueryController 业务组件。
 */
@RestController
@RequestMapping("/canvas/bi")
public class BiQueryController {

    private final TenantContextResolver tenantContextResolver;
    private final BiEmbedTicketService embedTicketService;
    private final BiQueryExecutionService queryExecutionService;
    private final BiQueryHistoryReader queryHistoryReader;
    private final BiDatasourceHealthProvider datasourceHealthProvider;
    private final BiDatasetSpecResolver datasetSpecResolver;
    private final CdpWarehouseFieldGovernanceService fieldGovernanceService;
    private final BiPermissionService permissionService;
    private final BiQueryGovernancePolicy queryGovernancePolicy;
    private final BiQueryGovernancePolicyService queryGovernancePolicyService;
    private final BiQueryCachePolicyService queryCachePolicyService;
    private final BiPortalRuntimeService portalRuntimeService;
    private final BiQueryCompiler compiler = new BiQueryCompiler();

    /**
     * 执行 BiQueryController 流程，围绕 bi query controller 完成校验、计算或结果组装。
     */
    public BiQueryController() {
        this(null, BiEmbedTicketService.testService(), BiQueryExecutionService.testService(),
                BiQueryHistoryReader.empty(), BiDatasourceHealthProvider.empty(), BiDatasetSpecResolver.builtIn(),
                (CdpWarehouseFieldGovernanceService) null, null);
    }

    /**
     * 执行 BiQueryController 流程，围绕 bi query controller 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public BiQueryController(TenantContextResolver tenantContextResolver) {
        this(tenantContextResolver, BiEmbedTicketService.testService(), BiQueryExecutionService.testService(),
                BiQueryHistoryReader.empty(), BiDatasourceHealthProvider.empty(), BiDatasetSpecResolver.builtIn(),
                (CdpWarehouseFieldGovernanceService) null, null);
    }

    /**
     * 执行 BiQueryController 流程，围绕 bi query controller 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param embedTicketService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryHistoryReader query history reader 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasourceHealthProvider datasource health provider 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasetSpecResolverProvider 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryGovernancePolicyProvider query governance policy provider 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param queryGovernancePolicyServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryCachePolicyServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalRuntimeServiceProvider 时间参数，用于计算窗口、过期或审计时间。
     */
    @Autowired
    public BiQueryController(TenantContextResolver tenantContextResolver,
                             BiEmbedTicketService embedTicketService,
                             BiQueryExecutionService queryExecutionService,
                             BiQueryHistoryReader queryHistoryReader,
                             BiDatasourceHealthProvider datasourceHealthProvider,
                             ObjectProvider<BiDatasetSpecResolver> datasetSpecResolverProvider,
                             ObjectProvider<CdpWarehouseFieldGovernanceService> fieldGovernanceServiceProvider,
                             ObjectProvider<BiPermissionService> permissionServiceProvider,
                             ObjectProvider<BiQueryGovernancePolicy> queryGovernancePolicyProvider,
                             ObjectProvider<BiQueryGovernancePolicyService> queryGovernancePolicyServiceProvider,
                             ObjectProvider<BiQueryCachePolicyService> queryCachePolicyServiceProvider,
                             ObjectProvider<BiPortalRuntimeService> portalRuntimeServiceProvider) {
        this(tenantContextResolver, embedTicketService, queryExecutionService, queryHistoryReader,
                datasourceHealthProvider,
                datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn),
                fieldGovernanceServiceProvider.getIfAvailable(),
                permissionServiceProvider.getIfAvailable(),
                queryGovernancePolicyProvider.getIfAvailable(BiQueryGovernancePolicy::defaults),
                queryGovernancePolicyServiceProvider.getIfAvailable(),
                queryCachePolicyServiceProvider == null ? null : queryCachePolicyServiceProvider.getIfAvailable(),
                portalRuntimeServiceProvider == null ? null : portalRuntimeServiceProvider.getIfAvailable());
    }

    /**
     * 执行 BiQueryController 流程，围绕 bi query controller 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param embedTicketService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryHistoryReader query history reader 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasourceHealthProvider datasource health provider 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     */
    public BiQueryController(TenantContextResolver tenantContextResolver,
                             BiEmbedTicketService embedTicketService,
                             BiQueryExecutionService queryExecutionService,
                             BiQueryHistoryReader queryHistoryReader,
                             BiDatasourceHealthProvider datasourceHealthProvider) {
        this(tenantContextResolver, embedTicketService, queryExecutionService, queryHistoryReader,
                datasourceHealthProvider, BiDatasetSpecResolver.builtIn(),
                (CdpWarehouseFieldGovernanceService) null, null);
    }

    /**
     * 执行 BiQueryController 流程，围绕 bi query controller 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param embedTicketService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryHistoryReader query history reader 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasourceHealthProvider datasource health provider 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryController(TenantContextResolver tenantContextResolver,
                             BiEmbedTicketService embedTicketService,
                             BiQueryExecutionService queryExecutionService,
                             BiQueryHistoryReader queryHistoryReader,
                             BiDatasourceHealthProvider datasourceHealthProvider,
                             BiDatasetSpecResolver datasetSpecResolver,
                             CdpWarehouseFieldGovernanceService fieldGovernanceService) {
        this(tenantContextResolver, embedTicketService, queryExecutionService, queryHistoryReader,
                datasourceHealthProvider, datasetSpecResolver, fieldGovernanceService, null);
    }

    /**
     * 执行 BiQueryController 流程，围绕 bi query controller 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param embedTicketService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryHistoryReader query history reader 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasourceHealthProvider datasource health provider 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryController(TenantContextResolver tenantContextResolver,
                             BiEmbedTicketService embedTicketService,
                             BiQueryExecutionService queryExecutionService,
                             BiQueryHistoryReader queryHistoryReader,
                             BiDatasourceHealthProvider datasourceHealthProvider,
                             BiDatasetSpecResolver datasetSpecResolver,
                             CdpWarehouseFieldGovernanceService fieldGovernanceService,
                             BiPermissionService permissionService) {
        this(tenantContextResolver, embedTicketService, queryExecutionService, queryHistoryReader,
                datasourceHealthProvider, datasetSpecResolver, fieldGovernanceService, permissionService,
                BiQueryGovernancePolicy.defaults());
    }

    /**
     * 执行 BiQueryController 流程，围绕 bi query controller 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param embedTicketService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryHistoryReader query history reader 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasourceHealthProvider datasource health provider 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryGovernancePolicy query governance policy 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     */
    public BiQueryController(TenantContextResolver tenantContextResolver,
                             BiEmbedTicketService embedTicketService,
                             BiQueryExecutionService queryExecutionService,
                             BiQueryHistoryReader queryHistoryReader,
                             BiDatasourceHealthProvider datasourceHealthProvider,
                             BiDatasetSpecResolver datasetSpecResolver,
                             CdpWarehouseFieldGovernanceService fieldGovernanceService,
                             BiPermissionService permissionService,
                             BiQueryGovernancePolicy queryGovernancePolicy) {
        this(tenantContextResolver, embedTicketService, queryExecutionService, queryHistoryReader,
                datasourceHealthProvider, datasetSpecResolver, fieldGovernanceService, permissionService,
                queryGovernancePolicy, null);
    }

    /**
     * 执行 BiQueryController 流程，围绕 bi query controller 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param embedTicketService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryHistoryReader query history reader 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasourceHealthProvider datasource health provider 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryGovernancePolicy query governance policy 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param queryGovernancePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryController(TenantContextResolver tenantContextResolver,
                             BiEmbedTicketService embedTicketService,
                             BiQueryExecutionService queryExecutionService,
                             BiQueryHistoryReader queryHistoryReader,
                             BiDatasourceHealthProvider datasourceHealthProvider,
                             BiDatasetSpecResolver datasetSpecResolver,
                             CdpWarehouseFieldGovernanceService fieldGovernanceService,
                             BiPermissionService permissionService,
                             BiQueryGovernancePolicy queryGovernancePolicy,
                             BiQueryGovernancePolicyService queryGovernancePolicyService) {
        this(tenantContextResolver, embedTicketService, queryExecutionService, queryHistoryReader,
                datasourceHealthProvider, datasetSpecResolver, fieldGovernanceService, permissionService,
                queryGovernancePolicy, queryGovernancePolicyService, null);
    }

    /**
     * 执行 BiQueryController 流程，围绕 bi query controller 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param embedTicketService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryHistoryReader query history reader 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasourceHealthProvider datasource health provider 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryGovernancePolicy query governance policy 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param queryGovernancePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryCachePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryController(TenantContextResolver tenantContextResolver,
                             BiEmbedTicketService embedTicketService,
                             BiQueryExecutionService queryExecutionService,
                             BiQueryHistoryReader queryHistoryReader,
                             BiDatasourceHealthProvider datasourceHealthProvider,
                             BiDatasetSpecResolver datasetSpecResolver,
                             CdpWarehouseFieldGovernanceService fieldGovernanceService,
                             BiPermissionService permissionService,
                             BiQueryGovernancePolicy queryGovernancePolicy,
                             BiQueryGovernancePolicyService queryGovernancePolicyService,
                             BiQueryCachePolicyService queryCachePolicyService) {
        this(tenantContextResolver, embedTicketService, queryExecutionService, queryHistoryReader,
                datasourceHealthProvider, datasetSpecResolver, fieldGovernanceService, permissionService,
                queryGovernancePolicy, queryGovernancePolicyService, queryCachePolicyService, null);
    }

    /**
     * 执行 BiQueryController 流程，围绕 bi query controller 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param embedTicketService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryHistoryReader query history reader 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasourceHealthProvider datasource health provider 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryGovernancePolicy query governance policy 参数，用于 BiQueryController 流程中的校验、计算或对象转换。
     * @param queryGovernancePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryCachePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiQueryController(TenantContextResolver tenantContextResolver,
                             BiEmbedTicketService embedTicketService,
                             BiQueryExecutionService queryExecutionService,
                             BiQueryHistoryReader queryHistoryReader,
                             BiDatasourceHealthProvider datasourceHealthProvider,
                             BiDatasetSpecResolver datasetSpecResolver,
                             CdpWarehouseFieldGovernanceService fieldGovernanceService,
                             BiPermissionService permissionService,
                             BiQueryGovernancePolicy queryGovernancePolicy,
                             BiQueryGovernancePolicyService queryGovernancePolicyService,
                             BiQueryCachePolicyService queryCachePolicyService,
                             BiPortalRuntimeService portalRuntimeService) {
        this.tenantContextResolver = tenantContextResolver;
        this.embedTicketService = embedTicketService;
        this.queryExecutionService = queryExecutionService;
        this.queryHistoryReader = queryHistoryReader;
        this.datasourceHealthProvider = datasourceHealthProvider;
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
        this.fieldGovernanceService = fieldGovernanceService;
        this.permissionService = permissionService;
        this.queryGovernancePolicy = queryGovernancePolicy == null
                ? BiQueryGovernancePolicy.defaults()
                : queryGovernancePolicy;
        this.queryGovernancePolicyService = queryGovernancePolicyService;
        this.queryCachePolicyService = queryCachePolicyService;
        this.portalRuntimeService = portalRuntimeService;
    }

    /**
     * 执行 BiQueryController 流程，围绕 bi query controller 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param embedTicketService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryController(TenantContextResolver tenantContextResolver,
                             BiEmbedTicketService embedTicketService) {
        this(tenantContextResolver, embedTicketService, BiQueryExecutionService.testService(),
                BiQueryHistoryReader.empty(), BiDatasourceHealthProvider.empty(), BiDatasetSpecResolver.builtIn(),
                (CdpWarehouseFieldGovernanceService) null, null);
    }

    /**
     * 执行 BiQueryController 流程，围绕 bi query controller 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param embedTicketService 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryController(TenantContextResolver tenantContextResolver,
                             BiEmbedTicketService embedTicketService,
                             BiQueryExecutionService queryExecutionService) {
        this(tenantContextResolver, embedTicketService, queryExecutionService,
                BiQueryHistoryReader.empty(), BiDatasourceHealthProvider.empty(), BiDatasetSpecResolver.builtIn(),
                (CdpWarehouseFieldGovernanceService) null, null);
    }

    /**
     * 查询或读取业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    @GetMapping("/datasets")
    public Mono<R<List<DatasetView>>> listDatasets() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(datasetSpecResolver.datasets(tenantId).stream()
                                .map(this::toView)
                                .toList()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 查询或读取业务数据。
     *
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 getDataset 流程生成的业务结果。
     */
    @GetMapping("/datasets/{datasetKey}")
    public Mono<R<DatasetView>> getDataset(@PathVariable String datasetKey) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(toView(datasetSpecResolver.dataset(datasetKey, tenantId))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 查询或读取业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    @GetMapping("/dashboards/presets")
    public Mono<R<List<BiDashboardPreset>>> listDashboardPresets() {
        return Mono.fromCallable(() -> R.ok(MarketingBiDashboardPresetRegistry.presets()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 查询或读取业务数据。
     *
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回 getDashboardPreset 流程生成的业务结果。
     */
    @GetMapping("/dashboards/presets/{dashboardKey}")
    public Mono<R<BiDashboardPreset>> getDashboardPreset(@PathVariable String dashboardKey) {
        return Mono.fromCallable(() -> R.ok(MarketingBiDashboardPresetRegistry.preset(dashboardKey)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 执行 compile 流程，围绕 compile 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 compile 流程生成的业务结果。
     */
    @PostMapping("/query/compile")
    public Mono<R<BiCompiledQuery>> compile(@RequestBody BiQueryRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    Long tenantId = normalizeTenant(context);
                    BiDatasetSpec dataset = datasetSpecResolver.dataset(request.datasetKey(), tenantId);
                    BiQueryContext queryContext = new BiQueryContext(
                            tenantId,
                            context.username(),
                            context.role());
                    BiQueryRequest scopedRequest = prepareQuery(dataset, request, queryContext);
                    enforceFieldPolicy(dataset, scopedRequest, context);
                    return R.ok(compiler.compile(dataset, scopedRequest, tenantId));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/query/execute")
    public Mono<R<BiQueryResult>> execute(@RequestBody BiQueryRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(queryExecutionService.execute(request,
                                new BiQueryContext(normalizeTenant(context), context.username(), context.role()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行 explain 流程，围绕 explain 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 explain 流程生成的业务结果。
     */
    @PostMapping("/query/explain")
    public Mono<R<BiQueryExplanation>> explain(@RequestBody BiQueryRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(queryExecutionService.explain(request,
                                new BiQueryContext(normalizeTenant(context), context.username(), context.role()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param sqlHash sql hash 参数，用于 cancelQuery 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    @PostMapping("/query/cancel/{sqlHash}")
    public Mono<R<BiQueryCancellationResult>> cancelQuery(@PathVariable String sqlHash) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(queryExecutionService.cancel(sqlHash,
                                new BiQueryContext(normalizeTenant(context), context.username(), context.role()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/query/execute-gated")
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<BiQueryExecutionService.GatedBiQueryResult>> executeGated(
            @RequestBody GatedQueryRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (request == null || request.query() == null) {
                        throw new IllegalArgumentException("query is required");
                    }
                    return R.ok(queryExecutionService.executeWithAvailabilityGate(
                            request.query(),
                            new BiQueryContext(normalizeTenant(context), context.username(), context.role()),
                            request.from(),
                            request.to(),
                            request.mode(),
                            request.allowWarn()));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/query/execute-contract-gated")
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<BiQueryExecutionService.ContractGatedBiQueryResult>> executeContractGated(
            @RequestBody ContractGatedQueryRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (request == null || request.query() == null) {
                        throw new IllegalArgumentException("query is required");
                    }
                    return R.ok(queryExecutionService.executeWithConsumerAvailabilityContract(
                            request.query(),
                            new BiQueryContext(normalizeTenant(context), context.username(), context.role()),
                            request.contractKey(),
                            request.from(),
                            request.to()));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 查询或读取业务数据。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    @GetMapping("/query/history")
    public Mono<R<List<BiQueryHistoryItem>>> queryHistory(@RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(queryHistoryReader.recent(tenantId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 查询或读取业务数据。
     *
     * @param historyId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    @GetMapping("/query/history/{historyId}")
    public Mono<R<BiQueryHistoryDetail>> queryHistoryDetail(@PathVariable Long historyId) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(queryHistoryReader
                        .detail(tenantId, historyId)
                        .orElseThrow(() -> new IllegalArgumentException("BI query history not found: " + historyId))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 查询或读取业务数据。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    @GetMapping("/query/governance-summary")
    public Mono<R<BiQueryGovernanceSummary>> queryGovernanceSummary(@RequestParam(defaultValue = "100") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(queryHistoryReader.governanceSummary(tenantId, limit, effectiveGovernancePolicy(tenantId))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 查询或读取业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    @GetMapping("/query/governance-policy")
    public Mono<R<BiQueryGovernancePolicyView>> queryGovernancePolicy() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(queryGovernancePolicyService == null
                                /**
                                 * 转换为接口返回或领域视图。
                                 *
                                 * @return 返回组装或转换后的结果对象。
                                 */
                                ? toPolicyView(queryGovernancePolicy)
                                : queryGovernancePolicyService.currentPolicyView(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/query/governance-policy")
    public Mono<R<BiQueryGovernancePolicyView>> upsertQueryGovernancePolicy(
            @RequestBody BiQueryGovernancePolicyUpdateCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAdmin(context);
                    if (queryGovernancePolicyService == null) {
                        throw new IllegalStateException("BI query governance policy service is not configured");
                    }
                    return R.ok(queryGovernancePolicyService.upsertPolicy(
                            normalizeTenant(context),
                            command,
                            context.username()));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 查询或读取业务数据。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    @GetMapping("/query/governance-audit")
    public Mono<R<List<BiQueryGovernanceAuditEntry>>> queryGovernanceAudit(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAdmin(context);
                    return R.ok(queryGovernancePolicyService == null
                            ? List.<BiQueryGovernanceAuditEntry>of()
                            : queryGovernancePolicyService.recentAudit(normalizeTenant(context), limit));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 查询或读取业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    @GetMapping("/query/cache-policy")
    public Mono<R<BiQueryCachePolicyView>> queryCachePolicy() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
                    if (queryCachePolicyService == null) {
                        return R.ok(new BiQueryCachePolicyView(true, 300L, "CACHE", List.of()));
                    }
                    return R.ok(queryCachePolicyService.currentPolicyView(tenantId));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/query/cache-policy")
    public Mono<R<BiQueryCachePolicyView>> upsertQueryCachePolicy(
            @RequestBody BiQueryCachePolicyUpdateCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAdmin(context);
                    if (queryCachePolicyService == null) {
                        throw new IllegalStateException("BI query cache policy service is not configured");
                    }
                    return R.ok(queryCachePolicyService.upsertPolicy(
                            normalizeTenant(context),
                            command,
                            context.username()));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行 invalidateQueryCache 流程，围绕 invalidate query cache 完成校验、计算或结果组装。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 invalidateQueryCache 流程生成的业务结果。
     */
    @PostMapping("/query/cache/invalidate")
    public Mono<R<BiQueryCacheInvalidationResult>> invalidateQueryCache(
            @RequestBody BiQueryCacheInvalidationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAdmin(context);
                    if (queryCachePolicyService == null) {
                        throw new IllegalStateException("BI query cache policy service is not configured");
                    }
                    return R.ok(queryCachePolicyService.invalidate(command));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 查询或读取业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    @GetMapping("/query/cache-stats")
    public Mono<R<BiQueryCacheStats>> queryCacheStats() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAdmin(context);
                    if (queryCachePolicyService == null) {
                        return R.ok(BiQueryResultCache.noop().stats());
                    }
                    return R.ok(queryCachePolicyService.cacheStats());
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行 datasourceHealth 流程，围绕 datasource health 完成校验、计算或结果组装。
     *
     * @return 返回 datasource health 汇总后的集合、分页或映射视图。
     */
    @GetMapping("/datasources/health")
    public Mono<R<List<BiDatasourceHealth>>> datasourceHealth() {
        return Mono.fromCallable(() -> R.ok(datasourceHealthProvider.health()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 执行 datasourceHealthHistory 流程，围绕 datasource health history 完成校验、计算或结果组装。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 datasource health history 汇总后的集合、分页或映射视图。
     */
    @GetMapping("/datasources/health/history")
    public Mono<R<List<BiDatasourceHealthSnapshot>>> datasourceHealthHistory(@RequestParam(defaultValue = "20") int limit) {
        return Mono.fromCallable(() -> R.ok(datasourceHealthProvider.healthHistory(limit)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 执行 datasourceHealthSlo 流程，围绕 datasource health slo 完成校验、计算或结果组装。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 datasourceHealthSlo 流程生成的业务结果。
     */
    @GetMapping("/datasources/health/slo")
    public Mono<R<BiDatasourceHealthSloSummary>> datasourceHealthSlo(@RequestParam(defaultValue = "100") int limit) {
        return Mono.fromCallable(() -> R.ok(datasourceHealthProvider.healthSlo(limit)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/embed-tickets")
    public Mono<R<BiEmbedTicket>> createEmbedTicket(@RequestBody BiEmbedTicketRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(embedTicketService.createTicket(
                                context.tenantId() == null ? 0L : context.tenantId(),
                                context.username() == null ? "system" : context.username(),
                                request)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param origin origin 参数，用于 verifyEmbedTicket 流程中的校验、计算或对象转换。
     * @param referer referer 参数，用于 verifyEmbedTicket 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    @PostMapping("/embed-tickets/verify")
    public Mono<R<BiEmbedTicketPayload>> verifyEmbedTicket(
            @RequestBody BiEmbedTicketVerifyRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "Referer", required = false) String referer) {
        return Mono.fromCallable(() -> R.ok(embedTicketService.verifyForUse(
                        request.ticket(),
                        origin == null || origin.isBlank() ? referer : origin)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回布尔判断结果。
     */
    public Mono<R<BiEmbedTicketPayload>> verifyEmbedTicket(BiEmbedTicketVerifyRequest request) {
        return Mono.fromCallable(() -> R.ok(embedTicketService.verify(request.ticket())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param origin origin 参数，用于 executeEmbedQuery 流程中的校验、计算或对象转换。
     * @param referer referer 参数，用于 executeEmbedQuery 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/embed/query/execute")
    public Mono<R<BiQueryResult>> executeEmbedQuery(
            @RequestBody EmbedQueryRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "Referer", required = false) String referer) {
        return Mono.fromCallable(() -> {
                    if (request == null || request.query() == null) {
                        throw new IllegalArgumentException("embed query is required");
                    }
                    BiEmbedTicketPayload preview = embedTicketService.verify(request.ticket());
                    enforceEmbedQueryScope(preview, request);
                    BiEmbedTicketPayload payload = embedTicketService.verifyForUse(
                            request.ticket(),
                            origin == null || origin.isBlank() ? referer : origin);
                    enforceEmbedQueryScope(payload, request);
                    return R.ok(queryExecutionService.execute(
                            request.query(),
                            new BiQueryContext(payload.tenantId(), payload.username(), RoleNames.OPERATOR)));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 执行 cleanupEmbedTickets 流程，围绕 cleanup embed tickets 完成校验、计算或结果组装。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 cleanupEmbedTickets 流程生成的业务结果。
     */
    @PostMapping("/embed-tickets/cleanup")
    public Mono<R<BiEmbedTokenCleanupResult>> cleanupEmbedTickets(@RequestParam(defaultValue = "100") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(embedTicketService.cleanupExpiredTokens(normalizeTenant(context), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 current tenant id 计算得到的数量、金额或指标值。
     */
    private Mono<Long> currentTenantId() {
        return currentTenant().map(this::normalizeTenant);
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
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
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    /**
     * 执行 effectiveGovernancePolicy 流程，围绕 effective governance policy 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 effectiveGovernancePolicy 流程生成的业务结果。
     */
    private BiQueryGovernancePolicy effectiveGovernancePolicy(Long tenantId) {
        return queryGovernancePolicyService == null
                ? queryGovernancePolicy
                : queryGovernancePolicyService.currentPolicy(tenantId);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param policy policy 参数，用于 toPolicyView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BiQueryGovernancePolicyView toPolicyView(BiQueryGovernancePolicy policy) {
        return new BiQueryGovernancePolicyView(
                policy.defaultTimeoutMs(),
                policy.defaultQuotaRows(),
                policy.datasets().entrySet().stream()
                        .map(entry -> new BiQueryGovernancePolicyView.DatasetPolicyView(
                                entry.getKey(),
                                entry.getValue().timeoutMs(),
                                entry.getValue().quotaRows()))
                        .toList());
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void requireAdmin(TenantContext context) {
        if (context == null || (!context.isTenantAdmin() && !context.isSuperAdmin())) {
            throw new org.springframework.security.access.AccessDeniedException("BI query governance policy requires admin role");
        }
    }

    /**
     * 执行 enforceFieldPolicy 流程，围绕 enforce field policy 完成校验、计算或结果组装。
     *
     * @param dataset dataset 参数，用于 enforceFieldPolicy 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void enforceFieldPolicy(BiDatasetSpec dataset, BiQueryRequest request, TenantContext context) {
        if (fieldGovernanceService == null) {
            return;
        }
        fieldGovernanceService.enforceBiQuery(
                dataset,
                request,
                new BiQueryContext(
                        normalizeTenant(context),
                        context == null ? "system" : context.username(),
                        context == null ? null : context.role()),
                CdpWarehouseFieldGovernanceService.ACTION_BI_COMPILE);
    }

    /**
     * 执行 enforceEmbedQueryScope 流程，围绕 enforce embed query scope 完成校验、计算或结果组装。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param request 请求对象，承载本次操作的输入参数。
     */
    private void enforceEmbedQueryScope(BiEmbedTicketPayload payload, EmbedQueryRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (payload == null) {
            throw new SecurityException("BI embed ticket is required");
        }
        if ("DASHBOARD".equalsIgnoreCase(payload.resourceType())) {
            if (!equalsIgnoreCase(payload.resourceType(), request.resourceType())
                    || !payload.resourceKey().equals(request.resourceKey())) {
                throw new SecurityException("BI embed query resource does not match ticket");
            }
            if (request.query() == null
                    || request.query().dashboardKey() == null
                    || !payload.resourceKey().equals(request.query().dashboardKey())) {
                throw new SecurityException("BI embed query dashboard does not match ticket resource");
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (!"PORTAL".equalsIgnoreCase(payload.resourceType())) {
            throw new SecurityException("BI embed query only supports dashboard or portal tickets");
        }
        if (!"DASHBOARD".equalsIgnoreCase(request.resourceType())) {
            throw new SecurityException("BI portal ticket can only execute dashboard menu queries");
        }
        if (request.query() == null
                || request.query().dashboardKey() == null
                || !request.resourceKey().equals(request.query().dashboardKey())) {
            throw new SecurityException("BI embed query dashboard does not match requested resource");
        }
        if (!portalContainsDashboard(payload, request.resourceKey())) {
            throw new SecurityException("BI embed query dashboard is not declared in portal menu");
        }
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param left left 参数，用于 equalsIgnoreCase 流程中的校验、计算或对象转换。
     * @param right right 参数，用于 equalsIgnoreCase 流程中的校验、计算或对象转换。
     * @return 返回 equals ignore case 的布尔判断结果。
     */
    private boolean equalsIgnoreCase(String left, String right) {
        return left == null ? right == null : left.equalsIgnoreCase(right);
    }

    /**
     * 执行 portalContainsDashboard 流程，围绕 portal contains dashboard 完成校验、计算或结果组装。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回 portal contains dashboard 的布尔判断结果。
     */
    private boolean portalContainsDashboard(BiEmbedTicketPayload payload, String dashboardKey) {
        if (portalRuntimeService == null) {
            throw new IllegalStateException("BI portal runtime service is required");
        }
        BiPortalResource portal = portalRuntimeService.getPublished(
                payload.tenantId(),
                payload.resourceKey(),
                new BiQueryContext(payload.tenantId(), payload.username(), RoleNames.OPERATOR));
        return portal.menus().stream()
                .anyMatch(menu -> isDashboardMenu(menu, dashboardKey));
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param menu menu 参数，用于 isDashboardMenu 流程中的校验、计算或对象转换。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回布尔判断结果。
     */
    private boolean isDashboardMenu(BiPortalMenuResource menu, String dashboardKey) {
        return menu != null
                && "DASHBOARD".equalsIgnoreCase(menu.resourceType())
                && dashboardKey != null
                && dashboardKey.equals(menu.resourceKey());
    }

    /**
     * 执行 prepareQuery 流程，围绕 prepare query 完成校验、计算或结果组装。
     *
     * @param dataset dataset 参数，用于 prepareQuery 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 prepareQuery 流程生成的业务结果。
     */
    private BiQueryRequest prepareQuery(BiDatasetSpec dataset, BiQueryRequest request, BiQueryContext context) {
        if (permissionService == null) {
            return request;
        }
        return permissionService.prepareQuery(dataset, request, context, BiPermissionService.ACTION_USE).request();
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param dataset dataset 参数，用于 toView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private DatasetView toView(BiDatasetSpec dataset) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new DatasetView(
                dataset.datasetKey(),
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                dataset.fields().values().stream()
                        .sorted(Comparator.comparing(BiFieldSpec::fieldKey))
                        .map(field -> new FieldView(field.fieldKey(), field.role(), field.valueType()))
                        .toList(),
                dataset.metrics().values().stream()
                        .sorted(Comparator.comparing(BiMetricSpec::metricKey))
                        .map(metric -> new MetricView(metric.metricKey(), metric.valueType()))
                        .toList()
        );
    }

    /**
     * DatasetView 数据记录。
     */
    public record DatasetView(
            String datasetKey,
            List<FieldView> fields,
            List<MetricView> metrics
    ) {
    }

    /**
     * FieldView 数据记录。
     */
    public record FieldView(
            String fieldKey,
            BiFieldSpec.Role role,
            String dataType
    ) {
    }

    /**
     * MetricView 数据记录。
     */
    public record MetricView(
            String metricKey,
            String dataType
    ) {
    }

    /**
     * GatedQueryRequest 数据记录。
     */
    public record GatedQueryRequest(
            BiQueryRequest query,
            LocalDateTime from,
            LocalDateTime to,
            String mode,
            boolean allowWarn
    ) {
    }

    /**
     * ContractGatedQueryRequest 数据记录。
     */
    public record ContractGatedQueryRequest(
            BiQueryRequest query,
            String contractKey,
            LocalDateTime from,
            LocalDateTime to
    ) {
    }

    /**
     * EmbedQueryRequest 数据记录。
     */
    public record EmbedQueryRequest(
            String ticket,
            String resourceType,
            String resourceKey,
            String widgetKey,
            BiQueryRequest query
    ) {
    }
}
