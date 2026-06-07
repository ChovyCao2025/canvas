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
    private final BiQueryCompiler compiler = new BiQueryCompiler();

    public BiQueryController() {
        this(null, BiEmbedTicketService.testService(), BiQueryExecutionService.testService(),
                BiQueryHistoryReader.empty(), BiDatasourceHealthProvider.empty(), BiDatasetSpecResolver.builtIn(),
                (CdpWarehouseFieldGovernanceService) null, null);
    }

    public BiQueryController(TenantContextResolver tenantContextResolver) {
        this(tenantContextResolver, BiEmbedTicketService.testService(), BiQueryExecutionService.testService(),
                BiQueryHistoryReader.empty(), BiDatasourceHealthProvider.empty(), BiDatasetSpecResolver.builtIn(),
                (CdpWarehouseFieldGovernanceService) null, null);
    }

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
                             ObjectProvider<BiQueryCachePolicyService> queryCachePolicyServiceProvider) {
        this(tenantContextResolver, embedTicketService, queryExecutionService, queryHistoryReader,
                datasourceHealthProvider,
                datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn),
                fieldGovernanceServiceProvider.getIfAvailable(),
                permissionServiceProvider.getIfAvailable(),
                queryGovernancePolicyProvider.getIfAvailable(BiQueryGovernancePolicy::defaults),
                queryGovernancePolicyServiceProvider.getIfAvailable(),
                queryCachePolicyServiceProvider == null ? null : queryCachePolicyServiceProvider.getIfAvailable());
    }

    public BiQueryController(TenantContextResolver tenantContextResolver,
                             BiEmbedTicketService embedTicketService,
                             BiQueryExecutionService queryExecutionService,
                             BiQueryHistoryReader queryHistoryReader,
                             BiDatasourceHealthProvider datasourceHealthProvider) {
        this(tenantContextResolver, embedTicketService, queryExecutionService, queryHistoryReader,
                datasourceHealthProvider, BiDatasetSpecResolver.builtIn(),
                (CdpWarehouseFieldGovernanceService) null, null);
    }

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
    }

    public BiQueryController(TenantContextResolver tenantContextResolver,
                             BiEmbedTicketService embedTicketService) {
        this(tenantContextResolver, embedTicketService, BiQueryExecutionService.testService(),
                BiQueryHistoryReader.empty(), BiDatasourceHealthProvider.empty(), BiDatasetSpecResolver.builtIn(),
                (CdpWarehouseFieldGovernanceService) null, null);
    }

    public BiQueryController(TenantContextResolver tenantContextResolver,
                             BiEmbedTicketService embedTicketService,
                             BiQueryExecutionService queryExecutionService) {
        this(tenantContextResolver, embedTicketService, queryExecutionService,
                BiQueryHistoryReader.empty(), BiDatasourceHealthProvider.empty(), BiDatasetSpecResolver.builtIn(),
                (CdpWarehouseFieldGovernanceService) null, null);
    }

    @GetMapping("/datasets")
    public Mono<R<List<DatasetView>>> listDatasets() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(datasetSpecResolver.datasets(tenantId).stream()
                                .map(this::toView)
                                .toList()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/datasets/{datasetKey}")
    public Mono<R<DatasetView>> getDataset(@PathVariable String datasetKey) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(toView(datasetSpecResolver.dataset(datasetKey, tenantId))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/dashboards/presets")
    public Mono<R<List<BiDashboardPreset>>> listDashboardPresets() {
        return Mono.fromCallable(() -> R.ok(MarketingBiDashboardPresetRegistry.presets()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/dashboards/presets/{dashboardKey}")
    public Mono<R<BiDashboardPreset>> getDashboardPreset(@PathVariable String dashboardKey) {
        return Mono.fromCallable(() -> R.ok(MarketingBiDashboardPresetRegistry.preset(dashboardKey)))
                .subscribeOn(Schedulers.boundedElastic());
    }

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

    @PostMapping("/query/execute")
    public Mono<R<BiQueryResult>> execute(@RequestBody BiQueryRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(queryExecutionService.execute(request,
                                new BiQueryContext(normalizeTenant(context), context.username(), context.role()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/query/explain")
    public Mono<R<BiQueryExplanation>> explain(@RequestBody BiQueryRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(queryExecutionService.explain(request,
                                new BiQueryContext(normalizeTenant(context), context.username(), context.role()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/query/cancel/{sqlHash}")
    public Mono<R<BiQueryCancellationResult>> cancelQuery(@PathVariable String sqlHash) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(queryExecutionService.cancel(sqlHash,
                                new BiQueryContext(normalizeTenant(context), context.username(), context.role()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/query/execute-gated")
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

    @GetMapping("/query/history")
    public Mono<R<List<BiQueryHistoryItem>>> queryHistory(@RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(queryHistoryReader.recent(tenantId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/query/history/{historyId}")
    public Mono<R<BiQueryHistoryDetail>> queryHistoryDetail(@PathVariable Long historyId) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(queryHistoryReader
                        .detail(tenantId, historyId)
                        .orElseThrow(() -> new IllegalArgumentException("BI query history not found: " + historyId))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/query/governance-summary")
    public Mono<R<BiQueryGovernanceSummary>> queryGovernanceSummary(@RequestParam(defaultValue = "100") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(queryHistoryReader.governanceSummary(tenantId, limit, effectiveGovernancePolicy(tenantId))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/query/governance-policy")
    public Mono<R<BiQueryGovernancePolicyView>> queryGovernancePolicy() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(queryGovernancePolicyService == null
                                ? toPolicyView(queryGovernancePolicy)
                                : queryGovernancePolicyService.currentPolicyView(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    @GetMapping("/datasources/health")
    public Mono<R<List<BiDatasourceHealth>>> datasourceHealth() {
        return Mono.fromCallable(() -> R.ok(datasourceHealthProvider.health()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/datasources/health/history")
    public Mono<R<List<BiDatasourceHealthSnapshot>>> datasourceHealthHistory(@RequestParam(defaultValue = "20") int limit) {
        return Mono.fromCallable(() -> R.ok(datasourceHealthProvider.healthHistory(limit)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/datasources/health/slo")
    public Mono<R<BiDatasourceHealthSloSummary>> datasourceHealthSlo(@RequestParam(defaultValue = "100") int limit) {
        return Mono.fromCallable(() -> R.ok(datasourceHealthProvider.healthSlo(limit)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/embed-tickets")
    public Mono<R<BiEmbedTicket>> createEmbedTicket(@RequestBody BiEmbedTicketRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(embedTicketService.createTicket(
                                context.tenantId() == null ? 0L : context.tenantId(),
                                context.username() == null ? "system" : context.username(),
                                request)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

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

    public Mono<R<BiEmbedTicketPayload>> verifyEmbedTicket(BiEmbedTicketVerifyRequest request) {
        return Mono.fromCallable(() -> R.ok(embedTicketService.verify(request.ticket())))
                .subscribeOn(Schedulers.boundedElastic());
    }

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

    @PostMapping("/embed-tickets/cleanup")
    public Mono<R<BiEmbedTokenCleanupResult>> cleanupEmbedTickets(@RequestParam(defaultValue = "100") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(embedTicketService.cleanupExpiredTokens(normalizeTenant(context), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<Long> currentTenantId() {
        return currentTenant().map(this::normalizeTenant);
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

    private BiQueryGovernancePolicy effectiveGovernancePolicy(Long tenantId) {
        return queryGovernancePolicyService == null
                ? queryGovernancePolicy
                : queryGovernancePolicyService.currentPolicy(tenantId);
    }

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

    private void requireAdmin(TenantContext context) {
        if (context == null || (!context.isTenantAdmin() && !context.isSuperAdmin())) {
            throw new org.springframework.security.access.AccessDeniedException("BI query governance policy requires admin role");
        }
    }

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

    private void enforceEmbedQueryScope(BiEmbedTicketPayload payload, EmbedQueryRequest request) {
        if (payload == null) {
            throw new SecurityException("BI embed ticket is required");
        }
        if (!"DASHBOARD".equalsIgnoreCase(payload.resourceType())) {
            throw new SecurityException("BI embed query only supports dashboard tickets");
        }
        if (!equalsIgnoreCase(payload.resourceType(), request.resourceType())
                || !payload.resourceKey().equals(request.resourceKey())) {
            throw new SecurityException("BI embed query resource does not match ticket");
        }
        if (request.query() == null
                || request.query().dashboardKey() == null
                || !payload.resourceKey().equals(request.query().dashboardKey())) {
            throw new SecurityException("BI embed query dashboard does not match ticket resource");
        }
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left == null ? right == null : left.equalsIgnoreCase(right);
    }

    private BiQueryRequest prepareQuery(BiDatasetSpec dataset, BiQueryRequest request, BiQueryContext context) {
        if (permissionService == null) {
            return request;
        }
        return permissionService.prepareQuery(dataset, request, context, BiPermissionService.ACTION_USE).request();
    }

    private DatasetView toView(BiDatasetSpec dataset) {
        return new DatasetView(
                dataset.datasetKey(),
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

    public record DatasetView(
            String datasetKey,
            List<FieldView> fields,
            List<MetricView> metrics
    ) {
    }

    public record FieldView(
            String fieldKey,
            BiFieldSpec.Role role,
            String dataType
    ) {
    }

    public record MetricView(
            String metricKey,
            String dataType
    ) {
    }

    public record GatedQueryRequest(
            BiQueryRequest query,
            LocalDateTime from,
            LocalDateTime to,
            String mode,
            boolean allowWarn
    ) {
    }

    public record ContractGatedQueryRequest(
            BiQueryRequest query,
            String contractKey,
            LocalDateTime from,
            LocalDateTime to
    ) {
    }

    public record EmbedQueryRequest(
            String ticket,
            String resourceType,
            String resourceKey,
            String widgetKey,
            BiQueryRequest query
    ) {
    }
}
