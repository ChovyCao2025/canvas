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

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * embedticket服务，用于承接对应业务能力和领域编排。
     */
    private final BiEmbedTicketService embedTicketService;
    /**
     * 查询执行服务，用于承接对应业务能力和领域编排。
     */
    private final BiQueryExecutionService queryExecutionService;
    /**
     * 查询历史读取器，用于保存请求处理过程中需要的业务数据。
     */
    private final BiQueryHistoryReader queryHistoryReader;
    /**
     * datasourcehealth提供方，用于保存请求处理过程中需要的业务数据。
     */
    private final BiDatasourceHealthProvider datasourceHealthProvider;
    /**
     * 数据集spec解析器，用于保存请求处理过程中需要的业务数据。
     */
    private final BiDatasetSpecResolver datasetSpecResolver;
    /**
     * fieldgovernance服务，用于承接对应业务能力和领域编排。
     */
    private final CdpWarehouseFieldGovernanceService fieldGovernanceService;
    /**
     * 权限服务，用于承接对应业务能力和领域编排。
     */
    private final BiPermissionService permissionService;
    /**
     * 查询governancepolicy，用于保存请求处理过程中需要的业务数据。
     */
    private final BiQueryGovernancePolicy queryGovernancePolicy;
    /**
     * 查询governancepolicy服务，用于承接对应业务能力和领域编排。
     */
    private final BiQueryGovernancePolicyService queryGovernancePolicyService;
    /**
     * 查询缓存policy服务，用于承接对应业务能力和领域编排。
     */
    private final BiQueryCachePolicyService queryCachePolicyService;
    /**
     * 门户运行态服务，用于承接对应业务能力和领域编排。
     */
    private final BiPortalRuntimeService portalRuntimeService;
    /**
     * compiler，用于保存请求处理过程中需要的业务数据。
     */
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
                        /**
                         * 执行 illegalargumentexception 对应的内部处理流程。
                         *
                         * @param required" required"，由调用方提供
                         * @return 返回内部处理结果
                         */
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
                        /**
                         * 执行 illegalargumentexception 对应的内部处理流程。
                         *
                         * @param required" required"，由调用方提供
                         * @return 返回内部处理结果
                         */
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
                        /**
                         * 执行 illegalstateexception 对应的内部处理流程。
                         *
                         * @param configured" configured"，由调用方提供
                         * @return 返回内部处理结果
                         */
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
                        /**
                         * 执行 illegalstateexception 对应的内部处理流程。
                         *
                         * @param configured" configured"，由调用方提供
                         * @return 返回内部处理结果
                         */
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
                        /**
                         * 执行 illegalstateexception 对应的内部处理流程。
                         *
                         * @param configured" configured"，由调用方提供
                         * @return 返回内部处理结果
                         */
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
                        /**
                         * 执行 illegalargumentexception 对应的内部处理流程。
                         *
                         * @param required" required"，由调用方提供
                         * @return 返回内部处理结果
                         */
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
     * 将治理策略领域对象转换为接口视图。
     *
     * @param policy 查询治理策略
     * @return 前端可消费的治理策略视图
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
            /**
             * 执行 securityexception 对应的内部处理流程。
             *
             * @param required" required"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new SecurityException("BI embed ticket is required");
        }
        if ("DASHBOARD".equalsIgnoreCase(payload.resourceType())) {
            if (!equalsIgnoreCase(payload.resourceType(), request.resourceType())
                    || !payload.resourceKey().equals(request.resourceKey())) {
                /**
                 * 执行 securityexception 对应的内部处理流程。
                 *
                 * @param ticket" ticket"，由调用方提供
                 * @return 返回内部处理结果
                 */
                throw new SecurityException("BI embed query resource does not match ticket");
            }
            if (request.query() == null
                    || request.query().dashboardKey() == null
                    || !payload.resourceKey().equals(request.query().dashboardKey())) {
                /**
                 * 执行 securityexception 对应的内部处理流程。
                 *
                 * @param resource" resource"，由调用方提供
                 * @return 返回内部处理结果
                 */
                throw new SecurityException("BI embed query dashboard does not match ticket resource");
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (!"PORTAL".equalsIgnoreCase(payload.resourceType())) {
            /**
             * 执行 securityexception 对应的内部处理流程。
             *
             * @param tickets" tickets"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new SecurityException("BI embed query only supports dashboard or portal tickets");
        }
        if (!"DASHBOARD".equalsIgnoreCase(request.resourceType())) {
            /**
             * 执行 securityexception 对应的内部处理流程。
             *
             * @param queries" queries"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new SecurityException("BI portal ticket can only execute dashboard menu queries");
        }
        if (request.query() == null
                || request.query().dashboardKey() == null
                || !request.resourceKey().equals(request.query().dashboardKey())) {
            /**
             * 执行 securityexception 对应的内部处理流程。
             *
             * @param resource" resource"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new SecurityException("BI embed query dashboard does not match requested resource");
        }
        if (!portalContainsDashboard(payload, request.resourceKey())) {
            /**
             * 执行 securityexception 对应的内部处理流程。
             *
             * @param menu" menu"，由调用方提供
             * @return 返回内部处理结果
             */
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
            /**
             * 执行 illegalstateexception 对应的内部处理流程。
             *
             * @param required" required"，由调用方提供
             * @return 返回内部处理结果
             */
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
    public static final class DatasetView {

        /**
         * datasetKey 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("datasetKey")
        private final String datasetKey;

        /**
         * fields 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("fields")
        private final List<FieldView> fields;

        /**
         * metrics 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("metrics")
        private final List<MetricView> metrics;

        /**
         * 创建 DatasetView 实例。
         *
         * @param datasetKey datasetKey 字段值
         * @param fields fields 字段值
         * @param metrics metrics 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public DatasetView(@com.fasterxml.jackson.annotation.JsonProperty("datasetKey") String datasetKey, @com.fasterxml.jackson.annotation.JsonProperty("fields") List<FieldView> fields, @com.fasterxml.jackson.annotation.JsonProperty("metrics") List<MetricView> metrics) {
            this.datasetKey = datasetKey;
            this.fields = fields;
            this.metrics = metrics;
        }

        /**
         * 返回datasetKey 字段值。
         *
         * @return datasetKey 字段值
         */
        public String datasetKey() {
            return datasetKey;
        }

        /**
         * 返回fields 字段值。
         *
         * @return fields 字段值
         */
        public List<FieldView> fields() {
            return fields;
        }

        /**
         * 返回metrics 字段值。
         *
         * @return metrics 字段值
         */
        public List<MetricView> metrics() {
            return metrics;
        }

        /**
         * 判断两个 DatasetView 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DatasetView that)) {
                return false;
            }
            return java.util.Objects.equals(datasetKey, that.datasetKey) && java.util.Objects.equals(fields, that.fields) && java.util.Objects.equals(metrics, that.metrics);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(datasetKey, fields, metrics);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "DatasetView[" + "datasetKey=" + datasetKey + ", " + "fields=" + fields + ", " + "metrics=" + metrics + "]";
        }
    }

    /**
     * FieldView 数据记录。
     */
    public static final class FieldView {

        /**
         * fieldKey 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("fieldKey")
        private final String fieldKey;

        /**
         * role 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("role")
        private final BiFieldSpec.Role role;

        /**
         * dataType 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("dataType")
        private final String dataType;

        /**
         * 创建 FieldView 实例。
         *
         * @param fieldKey fieldKey 字段值
         * @param role role 字段值
         * @param dataType dataType 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public FieldView(@com.fasterxml.jackson.annotation.JsonProperty("fieldKey") String fieldKey, @com.fasterxml.jackson.annotation.JsonProperty("role") BiFieldSpec.Role role, @com.fasterxml.jackson.annotation.JsonProperty("dataType") String dataType) {
            this.fieldKey = fieldKey;
            this.role = role;
            this.dataType = dataType;
        }

        /**
         * 返回fieldKey 字段值。
         *
         * @return fieldKey 字段值
         */
        public String fieldKey() {
            return fieldKey;
        }

        /**
         * 返回role 字段值。
         *
         * @return role 字段值
         */
        public BiFieldSpec.Role role() {
            return role;
        }

        /**
         * 返回dataType 字段值。
         *
         * @return dataType 字段值
         */
        public String dataType() {
            return dataType;
        }

        /**
         * 判断两个 FieldView 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FieldView that)) {
                return false;
            }
            return java.util.Objects.equals(fieldKey, that.fieldKey) && java.util.Objects.equals(role, that.role) && java.util.Objects.equals(dataType, that.dataType);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(fieldKey, role, dataType);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "FieldView[" + "fieldKey=" + fieldKey + ", " + "role=" + role + ", " + "dataType=" + dataType + "]";
        }
    }

    /**
     * MetricView 数据记录。
     */
    public static final class MetricView {

        /**
         * metricKey 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("metricKey")
        private final String metricKey;

        /**
         * dataType 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("dataType")
        private final String dataType;

        /**
         * 创建 MetricView 实例。
         *
         * @param metricKey metricKey 字段值
         * @param dataType dataType 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public MetricView(@com.fasterxml.jackson.annotation.JsonProperty("metricKey") String metricKey, @com.fasterxml.jackson.annotation.JsonProperty("dataType") String dataType) {
            this.metricKey = metricKey;
            this.dataType = dataType;
        }

        /**
         * 返回metricKey 字段值。
         *
         * @return metricKey 字段值
         */
        public String metricKey() {
            return metricKey;
        }

        /**
         * 返回dataType 字段值。
         *
         * @return dataType 字段值
         */
        public String dataType() {
            return dataType;
        }

        /**
         * 判断两个 MetricView 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MetricView that)) {
                return false;
            }
            return java.util.Objects.equals(metricKey, that.metricKey) && java.util.Objects.equals(dataType, that.dataType);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(metricKey, dataType);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "MetricView[" + "metricKey=" + metricKey + ", " + "dataType=" + dataType + "]";
        }
    }

    /**
     * GatedQueryRequest 数据记录。
     */
    public static final class GatedQueryRequest {

        /**
         * query 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("query")
        private final BiQueryRequest query;

        /**
         * from 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("from")
        private final LocalDateTime from;

        /**
         * to 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("to")
        private final LocalDateTime to;

        /**
         * mode 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("mode")
        private final String mode;

        /**
         * allowWarn 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("allowWarn")
        private final boolean allowWarn;

        /**
         * 创建 GatedQueryRequest 实例。
         *
         * @param query query 字段值
         * @param from from 字段值
         * @param to to 字段值
         * @param mode mode 字段值
         * @param allowWarn allowWarn 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public GatedQueryRequest(@com.fasterxml.jackson.annotation.JsonProperty("query") BiQueryRequest query, @com.fasterxml.jackson.annotation.JsonProperty("from") LocalDateTime from, @com.fasterxml.jackson.annotation.JsonProperty("to") LocalDateTime to, @com.fasterxml.jackson.annotation.JsonProperty("mode") String mode, @com.fasterxml.jackson.annotation.JsonProperty("allowWarn") boolean allowWarn) {
            this.query = query;
            this.from = from;
            this.to = to;
            this.mode = mode;
            this.allowWarn = allowWarn;
        }

        /**
         * 返回query 字段值。
         *
         * @return query 字段值
         */
        public BiQueryRequest query() {
            return query;
        }

        /**
         * 返回from 字段值。
         *
         * @return from 字段值
         */
        public LocalDateTime from() {
            return from;
        }

        /**
         * 返回to 字段值。
         *
         * @return to 字段值
         */
        public LocalDateTime to() {
            return to;
        }

        /**
         * 返回mode 字段值。
         *
         * @return mode 字段值
         */
        public String mode() {
            return mode;
        }

        /**
         * 返回allowWarn 字段值。
         *
         * @return allowWarn 字段值
         */
        public boolean allowWarn() {
            return allowWarn;
        }

        /**
         * 判断两个 GatedQueryRequest 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GatedQueryRequest that)) {
                return false;
            }
            return java.util.Objects.equals(query, that.query) && java.util.Objects.equals(from, that.from) && java.util.Objects.equals(to, that.to) && java.util.Objects.equals(mode, that.mode) && java.util.Objects.equals(allowWarn, that.allowWarn);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(query, from, to, mode, allowWarn);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "GatedQueryRequest[" + "query=" + query + ", " + "from=" + from + ", " + "to=" + to + ", " + "mode=" + mode + ", " + "allowWarn=" + allowWarn + "]";
        }
    }

    /**
     * ContractGatedQueryRequest 数据记录。
     */
    public static final class ContractGatedQueryRequest {

        /**
         * query 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("query")
        private final BiQueryRequest query;

        /**
         * contractKey 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("contractKey")
        private final String contractKey;

        /**
         * from 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("from")
        private final LocalDateTime from;

        /**
         * to 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("to")
        private final LocalDateTime to;

        /**
         * 创建 ContractGatedQueryRequest 实例。
         *
         * @param query query 字段值
         * @param contractKey contractKey 字段值
         * @param from from 字段值
         * @param to to 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public ContractGatedQueryRequest(@com.fasterxml.jackson.annotation.JsonProperty("query") BiQueryRequest query, @com.fasterxml.jackson.annotation.JsonProperty("contractKey") String contractKey, @com.fasterxml.jackson.annotation.JsonProperty("from") LocalDateTime from, @com.fasterxml.jackson.annotation.JsonProperty("to") LocalDateTime to) {
            this.query = query;
            this.contractKey = contractKey;
            this.from = from;
            this.to = to;
        }

        /**
         * 返回query 字段值。
         *
         * @return query 字段值
         */
        public BiQueryRequest query() {
            return query;
        }

        /**
         * 返回contractKey 字段值。
         *
         * @return contractKey 字段值
         */
        public String contractKey() {
            return contractKey;
        }

        /**
         * 返回from 字段值。
         *
         * @return from 字段值
         */
        public LocalDateTime from() {
            return from;
        }

        /**
         * 返回to 字段值。
         *
         * @return to 字段值
         */
        public LocalDateTime to() {
            return to;
        }

        /**
         * 判断两个 ContractGatedQueryRequest 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ContractGatedQueryRequest that)) {
                return false;
            }
            return java.util.Objects.equals(query, that.query) && java.util.Objects.equals(contractKey, that.contractKey) && java.util.Objects.equals(from, that.from) && java.util.Objects.equals(to, that.to);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(query, contractKey, from, to);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "ContractGatedQueryRequest[" + "query=" + query + ", " + "contractKey=" + contractKey + ", " + "from=" + from + ", " + "to=" + to + "]";
        }
    }

    /**
     * EmbedQueryRequest 数据记录。
     */
    public static final class EmbedQueryRequest {

        /**
         * ticket 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("ticket")
        private final String ticket;

        /**
         * resourceType 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("resourceType")
        private final String resourceType;

        /**
         * resourceKey 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("resourceKey")
        private final String resourceKey;

        /**
         * widgetKey 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("widgetKey")
        private final String widgetKey;

        /**
         * query 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("query")
        private final BiQueryRequest query;

        /**
         * 创建 EmbedQueryRequest 实例。
         *
         * @param ticket ticket 字段值
         * @param resourceType resourceType 字段值
         * @param resourceKey resourceKey 字段值
         * @param widgetKey widgetKey 字段值
         * @param query query 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public EmbedQueryRequest(@com.fasterxml.jackson.annotation.JsonProperty("ticket") String ticket, @com.fasterxml.jackson.annotation.JsonProperty("resourceType") String resourceType, @com.fasterxml.jackson.annotation.JsonProperty("resourceKey") String resourceKey, @com.fasterxml.jackson.annotation.JsonProperty("widgetKey") String widgetKey, @com.fasterxml.jackson.annotation.JsonProperty("query") BiQueryRequest query) {
            this.ticket = ticket;
            this.resourceType = resourceType;
            this.resourceKey = resourceKey;
            this.widgetKey = widgetKey;
            this.query = query;
        }

        /**
         * 返回ticket 字段值。
         *
         * @return ticket 字段值
         */
        public String ticket() {
            return ticket;
        }

        /**
         * 返回resourceType 字段值。
         *
         * @return resourceType 字段值
         */
        public String resourceType() {
            return resourceType;
        }

        /**
         * 返回resourceKey 字段值。
         *
         * @return resourceKey 字段值
         */
        public String resourceKey() {
            return resourceKey;
        }

        /**
         * 返回widgetKey 字段值。
         *
         * @return widgetKey 字段值
         */
        public String widgetKey() {
            return widgetKey;
        }

        /**
         * 返回query 字段值。
         *
         * @return query 字段值
         */
        public BiQueryRequest query() {
            return query;
        }

        /**
         * 判断两个 EmbedQueryRequest 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EmbedQueryRequest that)) {
                return false;
            }
            return java.util.Objects.equals(ticket, that.ticket) && java.util.Objects.equals(resourceType, that.resourceType) && java.util.Objects.equals(resourceKey, that.resourceKey) && java.util.Objects.equals(widgetKey, that.widgetKey) && java.util.Objects.equals(query, that.query);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(ticket, resourceType, resourceKey, widgetKey, query);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "EmbedQueryRequest[" + "ticket=" + ticket + ", " + "resourceType=" + resourceType + ", " + "resourceKey=" + resourceKey + ", " + "widgetKey=" + widgetKey + ", " + "query=" + query + "]";
        }
    }
}
