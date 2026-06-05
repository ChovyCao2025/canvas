package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardPreset;
import org.chovy.canvas.domain.bi.dashboard.MarketingBiDashboardPresetRegistry;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicket;
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
import org.chovy.canvas.domain.bi.query.BiFieldSpec;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryCompiler;
import org.chovy.canvas.domain.bi.query.BiQueryExecutionService;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryItem;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryReader;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
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
                             ObjectProvider<BiPermissionService> permissionServiceProvider) {
        this(tenantContextResolver, embedTicketService, queryExecutionService, queryHistoryReader,
                datasourceHealthProvider,
                datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn),
                fieldGovernanceServiceProvider.getIfAvailable(),
                permissionServiceProvider.getIfAvailable());
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
        this.tenantContextResolver = tenantContextResolver;
        this.embedTicketService = embedTicketService;
        this.queryExecutionService = queryExecutionService;
        this.queryHistoryReader = queryHistoryReader;
        this.datasourceHealthProvider = datasourceHealthProvider;
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
        this.fieldGovernanceService = fieldGovernanceService;
        this.permissionService = permissionService;
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

    @GetMapping("/datasources/health")
    public Mono<R<List<BiDatasourceHealth>>> datasourceHealth() {
        return Mono.fromCallable(() -> R.ok(datasourceHealthProvider.health()))
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
    public Mono<R<BiEmbedTicketPayload>> verifyEmbedTicket(@RequestBody BiEmbedTicketVerifyRequest request) {
        return Mono.fromCallable(() -> R.ok(embedTicketService.verify(request.ticket())))
                .subscribeOn(Schedulers.boundedElastic());
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
}
