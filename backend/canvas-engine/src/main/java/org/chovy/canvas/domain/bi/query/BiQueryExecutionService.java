package org.chovy.canvas.domain.bi.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationService;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineAdmissionDecision;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityService;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueAdmissionCommand;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueJobView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueService;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BiQueryExecutionService {

    private final BiQueryCompiler compiler;
    private final BiQueryExecutor executor;
    private final BiQueryHistoryRecorder historyRecorder;
    private final BiQueryResultCache resultCache;
    private final BiDatasetSpecResolver datasetSpecResolver;
    private final Clock clock;
    private final CdpWarehouseFieldGovernanceService fieldGovernanceService;
    private final BiPermissionService permissionService;
    private final CdpWarehouseAvailabilityService availabilityService;
    private final CdpWarehouseConsumerAvailabilityService consumerAvailabilityService;
    private final BiAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final BiQueryCachePolicyService cachePolicyService;
    private final BiDatasetAccelerationService datasetAccelerationService;
    private final BiQueryGovernancePolicyService governancePolicyService;
    private final BiQuickEngineCapacityService quickEngineCapacityService;
    private final BiQuickEngineQueueService quickEngineQueueService;

    @Autowired
    public BiQueryExecutionService(BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   ObjectProvider<BiDatasetSpecResolver> datasetSpecResolverProvider,
                                   ObjectProvider<CdpWarehouseFieldGovernanceService> fieldGovernanceServiceProvider,
                                   ObjectProvider<BiPermissionService> permissionServiceProvider,
                                   ObjectProvider<CdpWarehouseAvailabilityService> availabilityServiceProvider,
                                   ObjectProvider<CdpWarehouseConsumerAvailabilityService>
                                           consumerAvailabilityServiceProvider,
                                   ObjectProvider<BiAuditLogMapper> auditLogMapperProvider,
                                   ObjectProvider<ObjectMapper> objectMapperProvider,
                                   ObjectProvider<BiQueryGovernancePolicyService> governancePolicyServiceProvider,
                                   ObjectProvider<BiQueryCachePolicyService> cachePolicyServiceProvider,
                                   ObjectProvider<BiDatasetAccelerationService> datasetAccelerationServiceProvider,
                                   ObjectProvider<BiQuickEngineCapacityService> quickEngineCapacityServiceProvider,
                                   ObjectProvider<BiQuickEngineQueueService> quickEngineQueueServiceProvider) {
        this(new BiQueryCompiler(), executor, historyRecorder, resultCache,
                datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn),
                fieldGovernanceServiceProvider.getIfAvailable(),
                permissionServiceProvider.getIfAvailable(),
                availabilityServiceProvider == null ? null : availabilityServiceProvider.getIfAvailable(),
                consumerAvailabilityServiceProvider == null
                        ? null
                        : consumerAvailabilityServiceProvider.getIfAvailable(),
                Clock.systemUTC(),
                auditLogMapperProvider == null ? null : auditLogMapperProvider.getIfAvailable(),
                objectMapperProvider == null ? null : objectMapperProvider.getIfAvailable(),
                cachePolicyServiceProvider == null ? null : cachePolicyServiceProvider.getIfAvailable(),
                datasetAccelerationServiceProvider == null ? null : datasetAccelerationServiceProvider.getIfAvailable(),
                governancePolicyServiceProvider == null ? null : governancePolicyServiceProvider.getIfAvailable(),
                quickEngineCapacityServiceProvider == null ? null : quickEngineCapacityServiceProvider.getIfAvailable(),
                quickEngineQueueServiceProvider == null ? null : quickEngineQueueServiceProvider.getIfAvailable());
    }

    public BiQueryExecutionService(BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   ObjectProvider<CdpWarehouseFieldGovernanceService> fieldGovernanceServiceProvider) {
        this(new BiQueryCompiler(), executor, historyRecorder, resultCache,
                BiDatasetSpecResolver.builtIn(), fieldGovernanceServiceProvider.getIfAvailable(), null, null,
                Clock.systemUTC());
    }

    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   Clock clock) {
        this(compiler, executor, historyRecorder, BiQueryResultCache.noop(), clock);
    }

    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper) {
        this(compiler, executor, historyRecorder, BiQueryResultCache.noop(), BiDatasetSpecResolver.builtIn(),
                null, null, null, null, clock, auditLogMapper, objectMapper);
    }

    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   Clock clock) {
        this(compiler, executor, historyRecorder, resultCache, BiDatasetSpecResolver.builtIn(), null, null, null,
                clock);
    }

    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   Clock clock) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService, null, null,
                clock);
    }

    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   Clock clock) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, null, clock);
    }

    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   Clock clock) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, null, clock);
    }

    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, consumerAvailabilityService, clock, null, null);
    }

    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, consumerAvailabilityService, clock, auditLogMapper,
                objectMapper, null);
    }

    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper,
                                   BiQueryCachePolicyService cachePolicyService) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, consumerAvailabilityService, clock, auditLogMapper,
                objectMapper, cachePolicyService, null);
    }

    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper,
                                   BiQueryCachePolicyService cachePolicyService,
                                   BiDatasetAccelerationService datasetAccelerationService) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, consumerAvailabilityService, clock, auditLogMapper,
                objectMapper, cachePolicyService, datasetAccelerationService, null);
    }

    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper,
                                   BiQueryCachePolicyService cachePolicyService,
                                   BiDatasetAccelerationService datasetAccelerationService,
                                   BiQueryGovernancePolicyService governancePolicyService) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, consumerAvailabilityService, clock, auditLogMapper,
                objectMapper, cachePolicyService, datasetAccelerationService, governancePolicyService, null);
    }

    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper,
                                   BiQueryCachePolicyService cachePolicyService,
                                   BiDatasetAccelerationService datasetAccelerationService,
                                   BiQueryGovernancePolicyService governancePolicyService,
                                   BiQuickEngineCapacityService quickEngineCapacityService) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, consumerAvailabilityService, clock, auditLogMapper,
                objectMapper, cachePolicyService, datasetAccelerationService, governancePolicyService,
                quickEngineCapacityService, null);
    }

    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper,
                                   BiQueryCachePolicyService cachePolicyService,
                                   BiDatasetAccelerationService datasetAccelerationService,
                                   BiQueryGovernancePolicyService governancePolicyService,
                                   BiQuickEngineCapacityService quickEngineCapacityService,
                                   BiQuickEngineQueueService quickEngineQueueService) {
        this.compiler = compiler;
        this.executor = executor;
        this.historyRecorder = historyRecorder;
        this.resultCache = resultCache;
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
        this.fieldGovernanceService = fieldGovernanceService;
        this.permissionService = permissionService;
        this.availabilityService = availabilityService;
        this.consumerAvailabilityService = consumerAvailabilityService;
        this.clock = clock;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.cachePolicyService = cachePolicyService;
        this.datasetAccelerationService = datasetAccelerationService;
        this.governancePolicyService = governancePolicyService;
        this.quickEngineCapacityService = quickEngineCapacityService;
        this.quickEngineQueueService = quickEngineQueueService;
    }

    public static BiQueryExecutionService testService() {
        return new BiQueryExecutionService(
                new BiQueryCompiler(),
                (query, dataset) -> List.of(),
                BiQueryHistoryRecorder.noop(),
                BiQueryResultCache.noop(),
                Clock.systemUTC());
    }

    public BiQueryResult execute(BiQueryRequest request, BiQueryContext context) {
        BiDatasetSpec dataset = effectiveDataset(datasetSpecResolver.dataset(request.datasetKey(), context.tenantId()),
                context.tenantId());
        long startedAtMs = clock.instant().toEpochMilli();
        enforceGovernancePolicy(request, context, startedAtMs);
        BiPermissionService.BiPreparedQuery prepared = prepareQuery(request, context, dataset, startedAtMs);
        BiQueryRequest scopedRequest = prepared.request();
        enforceFieldPolicy(scopedRequest, context, dataset, startedAtMs);
        BiCompiledQuery query = compiler.compile(dataset, scopedRequest, context.tenantId());
        String sqlHash = hash(query, prepared.permissionSignature());
        BiQueryCachePolicy.ResourcePolicy cachePolicy = effectiveCachePolicy(
                context.tenantId(),
                scopedRequest.datasetKey(),
                scopedRequest.dashboardKey());
        BiQueryResult cachedResult = shouldUseCache(cachePolicy)
                ? resultCache.get(sqlHash)
                .map(result -> result.asCached(Math.max(0, clock.instant().toEpochMilli() - startedAtMs)))
                .orElse(null)
                : null;
        if (cachedResult != null) {
            recordHistory(new BiQueryHistoryEntry(
                    context.tenantId(),
                    context.username(),
                    scopedRequest,
                    sqlHash,
                    cachedResult.rowCount(),
                    cachedResult.durationMs(),
                    "CACHE_HIT",
                    null));
            return cachedResult;
        }
        BiQuickEngineAdmissionState quickEngineAdmission =
                enforceQuickEngineAdmission(scopedRequest, context, sqlHash, startedAtMs);
        try {
            List<Map<String, Object>> rows = executor.execute(query, dataset, sqlHash);
            List<Map<String, Object>> visibleRows = applyMasks(rows, prepared.columnMasks());
            long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
            BiQueryResult result = new BiQueryResult(
                    dataset.datasetKey(),
                    columns(dataset, scopedRequest),
                    visibleRows,
                    visibleRows.size(),
                    durationMs,
                    sqlHash);
            if (shouldUseCache(cachePolicy)) {
                resultCache.put(sqlHash, result, Duration.ofSeconds(cachePolicy.ttlSeconds()));
            }
            recordHistory(new BiQueryHistoryEntry(
                    context.tenantId(),
                    context.username(),
                    scopedRequest,
                    sqlHash,
                    result.rowCount(),
                    durationMs,
                    "SUCCESS",
                    null));
            completeQueuedAdmission(context, quickEngineAdmission);
            return result;
        } catch (RuntimeException e) {
            blockQueuedAdmission(context, quickEngineAdmission, e.getMessage());
            long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
            recordHistory(new BiQueryHistoryEntry(
                    context.tenantId(),
                    context.username(),
                    scopedRequest,
                    sqlHash,
                    0,
                    durationMs,
                    "FAILED",
                    e.getMessage()));
            throw e;
        } finally {
            if (quickEngineAdmission.admitted()) {
                quickEngineCapacityService.releaseQuery(context.tenantId());
            }
        }
    }

    public BiQueryExplanation explain(BiQueryRequest request, BiQueryContext context) {
        BiDatasetSpec dataset = effectiveDataset(datasetSpecResolver.dataset(request.datasetKey(), context.tenantId()),
                context.tenantId());
        long startedAtMs = clock.instant().toEpochMilli();
        BiPermissionService.BiPreparedQuery prepared = prepareQuery(request, context, dataset, startedAtMs);
        BiQueryRequest scopedRequest = prepared.request();
        enforceFieldPolicy(scopedRequest, context, dataset, startedAtMs);
        BiCompiledQuery query = compiler.compile(dataset, scopedRequest, context.tenantId());
        return new BiQueryExplanation(
                dataset.datasetKey(),
                hash(query, prepared.permissionSignature()),
                query.parameters().size(),
                executor.explain(query, dataset));
    }

    public BiQueryCancellationResult cancel(String sqlHash) {
        return cancel(sqlHash, new BiQueryContext(0L, "system"));
    }

    public BiQueryCancellationResult cancel(String sqlHash, BiQueryContext context) {
        if (sqlHash == null || sqlHash.isBlank()) {
            return new BiQueryCancellationResult("", false, "sqlHash is required");
        }
        boolean cancelled = executor.cancel(sqlHash);
        BiQueryCancellationResult result = new BiQueryCancellationResult(
                sqlHash,
                cancelled,
                cancelled ? "cancellation requested" : "not running");
        auditCancellation(result, context);
        return result;
    }

    public GatedBiQueryResult executeWithAvailabilityGate(BiQueryRequest request,
                                                          BiQueryContext context,
                                                          LocalDateTime from,
                                                          LocalDateTime to,
                                                          String mode,
                                                          boolean allowWarn) {
        if (request == null) {
            throw new IllegalArgumentException("query is required");
        }
        if (availabilityService == null) {
            throw new IllegalStateException("warehouse availability service is not configured");
        }
        Long tenantId = context == null || context.tenantId() == null ? 0L : context.tenantId();
        String username = context == null || context.username() == null ? "system" : context.username();
        long startedAtMs = clock.instant().toEpochMilli();
        CdpWarehouseAvailabilityService.AvailabilityDecision availability =
                availabilityService.evaluate(tenantId, from, to, mode);
        String availabilityStatus = availability == null ? "FAIL" : availability.status();
        if ("FAIL".equalsIgnoreCase(availabilityStatus)) {
            return blockedResult(tenantId, username, request, availability, startedAtMs,
                    "warehouse availability FAIL");
        }
        if ("WARN".equalsIgnoreCase(availabilityStatus) && !allowWarn) {
            return blockedResult(tenantId, username, request, availability, startedAtMs,
                    "warehouse availability WARN requires allowWarn=true");
        }
        BiQueryResult queryResult = execute(request, context == null ? new BiQueryContext(0L, "system") : context);
        String reason = "WARN".equalsIgnoreCase(availabilityStatus)
                ? "warehouse availability WARN accepted by operator"
                : "warehouse availability PASS";
        return new GatedBiQueryResult(
                tenantId,
                request.datasetKey(),
                "EXECUTED",
                reason,
                availability,
                queryResult);
    }

    public ContractGatedBiQueryResult executeWithConsumerAvailabilityContract(
            BiQueryRequest request,
            BiQueryContext context,
            String contractKey,
            LocalDateTime from,
            LocalDateTime to) {
        if (request == null) {
            throw new IllegalArgumentException("query is required");
        }
        if (contractKey == null || contractKey.isBlank()) {
            throw new IllegalArgumentException("contractKey is required");
        }
        if (consumerAvailabilityService == null) {
            throw new IllegalStateException("warehouse consumer availability service is not configured");
        }
        Long tenantId = context == null || context.tenantId() == null ? 0L : context.tenantId();
        String username = context == null || context.username() == null ? "system" : context.username();
        long startedAtMs = clock.instant().toEpochMilli();
        CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation =
                consumerAvailabilityService.evaluateContract(tenantId, contractKey, from, to);
        if (evaluation == null || !evaluation.allowed()) {
            String reason = evaluation == null
                    ? "consumer availability contract evaluation failed"
                    : evaluation.message();
            long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
            recordHistory(new BiQueryHistoryEntry(
                    tenantId,
                    username,
                    request,
                    requestHash("WAREHOUSE_CONSUMER_CONTRACT_BLOCKED:" + contractKey, request),
                    0,
                    durationMs,
                    "BLOCKED",
                    reason));
            return new ContractGatedBiQueryResult(
                    tenantId,
                    request.datasetKey(),
                    contractKey,
                    "BLOCKED",
                    reason,
                    evaluation,
                    null);
        }
        BiQueryResult queryResult = execute(request, context == null ? new BiQueryContext(0L, "system") : context);
        return new ContractGatedBiQueryResult(
                tenantId,
                request.datasetKey(),
                contractKey,
                "EXECUTED",
                evaluation.message(),
                evaluation,
                queryResult);
    }

    private GatedBiQueryResult blockedResult(Long tenantId,
                                             String username,
                                             BiQueryRequest request,
                                             CdpWarehouseAvailabilityService.AvailabilityDecision availability,
                                             long startedAtMs,
                                             String reason) {
        long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
        recordHistory(new BiQueryHistoryEntry(
                tenantId,
                username,
                request,
                requestHash("WAREHOUSE_AVAILABILITY_BLOCKED", request),
                0,
                durationMs,
                "BLOCKED",
                reason));
        return new GatedBiQueryResult(
                tenantId,
                request.datasetKey(),
                "BLOCKED",
                reason,
                availability,
                null);
    }

    private BiPermissionService.BiPreparedQuery prepareQuery(BiQueryRequest request,
                                                             BiQueryContext context,
                                                             BiDatasetSpec dataset,
                                                             long startedAtMs) {
        if (permissionService == null) {
            return new BiPermissionService.BiPreparedQuery(request, List.of(), "bi-permission:none");
        }
        try {
            return permissionService.prepareQuery(dataset, request, context, BiPermissionService.ACTION_USE);
        } catch (RuntimeException e) {
            long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
            recordHistory(new BiQueryHistoryEntry(
                    context.tenantId(),
                    context.username(),
                    request,
                    requestHash(request),
                    0,
                    durationMs,
                    "FAILED",
                    e.getMessage()));
            throw e;
        }
    }

    private BiQueryCachePolicy.ResourcePolicy effectiveCachePolicy(Long tenantId, String datasetKey, String dashboardKey) {
        if (cachePolicyService == null) {
            return BiQueryCachePolicy.defaults().defaultPolicy();
        }
        return cachePolicyService.effectivePolicy(tenantId, datasetKey, dashboardKey);
    }

    private BiDatasetSpec effectiveDataset(BiDatasetSpec dataset, Long tenantId) {
        if (datasetAccelerationService == null) {
            return dataset;
        }
        return datasetAccelerationService.applyAcceleration(tenantId, dataset);
    }

    private boolean shouldUseCache(BiQueryCachePolicy.ResourcePolicy cachePolicy) {
        return cachePolicy == null
                || (cachePolicy.enabled()
                && !BiQueryCachePolicy.MODE_DIRECT_QUERY.equals(cachePolicy.cacheMode()));
    }

    private List<Map<String, Object>> applyMasks(List<Map<String, Object>> rows,
                                                 List<BiPermissionService.BiColumnMask> masks) {
        if (permissionService == null) {
            return rows;
        }
        return permissionService.applyMasks(rows, masks);
    }

    private void enforceFieldPolicy(BiQueryRequest request,
                                    BiQueryContext context,
                                    BiDatasetSpec dataset,
                                    long startedAtMs) {
        if (fieldGovernanceService == null) {
            return;
        }
        try {
            fieldGovernanceService.enforceBiQuery(
                    dataset,
                    request,
                    context,
                    CdpWarehouseFieldGovernanceService.ACTION_BI_EXECUTE);
        } catch (RuntimeException e) {
            long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
            recordHistory(new BiQueryHistoryEntry(
                    context.tenantId(),
                    context.username(),
                    request,
                    requestHash(request),
                    0,
                    durationMs,
                    "FAILED",
                    e.getMessage()));
            throw e;
        }
    }

    private void enforceGovernancePolicy(BiQueryRequest request,
                                         BiQueryContext context,
                                         long startedAtMs) {
        if (governancePolicyService == null) {
            return;
        }
        BiQueryGovernancePolicy policy = governancePolicyService.currentPolicy(context.tenantId());
        BiQueryGovernancePolicy.DatasetPolicy datasetPolicy =
                (policy == null ? BiQueryGovernancePolicy.defaults() : policy).datasetPolicy(request.datasetKey());
        if (request.limit() <= datasetPolicy.quotaRows()) {
            return;
        }
        String message = "BI query limit exceeds governance quota for dataset " + request.datasetKey();
        long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
        recordHistory(new BiQueryHistoryEntry(
                context.tenantId(),
                context.username(),
                request,
                requestHash("BI_QUERY_GOVERNANCE_QUOTA", request),
                0,
                durationMs,
                "BLOCKED",
                message));
        throw new IllegalArgumentException(message);
    }

    private BiQuickEngineAdmissionState enforceQuickEngineAdmission(BiQueryRequest request,
                                                                    BiQueryContext context,
                                                                    String sqlHash,
                                                                    long startedAtMs) {
        if (quickEngineCapacityService == null) {
            return BiQuickEngineAdmissionState.notAdmitted();
        }
        BiQuickEngineAdmissionDecision decision = quickEngineCapacityService.admitQueryOrWait(context.tenantId(), 50);
        if (decision == null || decision.allowed()) {
            Long queuedJobId = null;
            if (isQueuedAdmission(decision)) {
                queuedJobId = persistQueuedAdmission(request, context, sqlHash, decision);
                long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
                recordHistory(new BiQueryHistoryEntry(
                        context.tenantId(),
                        context.username(),
                        request,
                        sqlHash,
                        0,
                        durationMs,
                        "QUEUED",
                        decision.message()));
            }
            return new BiQuickEngineAdmissionState(decision != null, queuedJobId);
        }
        String status = decision.status() == null || decision.status().isBlank()
                ? "BLOCKED"
                : decision.status().trim().toUpperCase(Locale.ROOT);
        String message = decision.message() == null || decision.message().isBlank()
                ? "Quick Engine tenant pool admission denied"
                : decision.message();
        long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
        recordHistory(new BiQueryHistoryEntry(
                context.tenantId(),
                context.username(),
                request,
                sqlHash,
                0,
                durationMs,
                status,
                message));
        throw new IllegalStateException(message);
    }

    private boolean isQueuedAdmission(BiQuickEngineAdmissionDecision decision) {
        return decision != null
                && ("ADMITTED_AFTER_QUEUE".equals(normalize(decision.status())) || decision.queued());
    }

    private Long persistQueuedAdmission(BiQueryRequest request,
                                        BiQueryContext context,
                                        String sqlHash,
                                        BiQuickEngineAdmissionDecision decision) {
        if (quickEngineQueueService == null) {
            return null;
        }
        String poolKey = decision.tenantPoolPolicy() == null ? null : decision.tenantPoolPolicy().poolKey();
        Integer queueTimeoutSeconds = decision.tenantPoolPolicy() == null
                ? null
                : decision.tenantPoolPolicy().queueTimeoutSeconds();
        try {
            BiQuickEngineQueueJobView job = quickEngineQueueService.enqueue(context.tenantId(), new BiQuickEngineQueueAdmissionCommand(
                    poolKey,
                    sqlHash,
                    request.datasetKey(),
                    context.username(),
                    queueTimeoutSeconds));
            return job == null ? null : job.id();
        } catch (RuntimeException ignored) {
            // Queue persistence is operational evidence; admission has already been granted.
            return null;
        }
    }

    private void completeQueuedAdmission(BiQueryContext context, BiQuickEngineAdmissionState admission) {
        if (quickEngineQueueService == null || admission.queuedJobId() == null) {
            return;
        }
        try {
            quickEngineQueueService.completeQueuedAdmission(context.tenantId(), admission.queuedJobId());
        } catch (RuntimeException ignored) {
            // Queue persistence is operational evidence; query success must remain authoritative.
        }
    }

    private void blockQueuedAdmission(BiQueryContext context,
                                      BiQuickEngineAdmissionState admission,
                                      String reason) {
        if (quickEngineQueueService == null || admission.queuedJobId() == null) {
            return;
        }
        try {
            quickEngineQueueService.blockQueuedAdmission(context.tenantId(), admission.queuedJobId(), reason);
        } catch (RuntimeException ignored) {
            // Queue persistence is operational evidence; datasource failure remains authoritative.
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record BiQuickEngineAdmissionState(boolean admitted, Long queuedJobId) {
        static BiQuickEngineAdmissionState notAdmitted() {
            return new BiQuickEngineAdmissionState(false, null);
        }
    }

    private void recordHistory(BiQueryHistoryEntry entry) {
        try {
            historyRecorder.record(entry);
        } catch (RuntimeException ignored) {
            // Query history is audit metadata; a recorder outage must not break report reads.
        }
    }

    private void auditCancellation(BiQueryCancellationResult result, BiQueryContext context) {
        if (auditLogMapper == null || result == null) {
            return;
        }
        BiQueryContext scopedContext = context == null ? new BiQueryContext(0L, "system") : context;
        BiAuditLogDO row = new BiAuditLogDO();
        row.setTenantId(scopedContext.tenantId());
        row.setActorId(scopedContext.username());
        row.setActionKey("BI_QUERY_CANCEL_REQUEST");
        row.setResourceType("BI_QUERY");
        row.setDetailJson(toJson(Map.of(
                "sqlHash", result.sqlHash(),
                "cancelled", result.cancelled(),
                "message", result.message())));
        row.setCreatedAt(LocalDateTime.now(clock));
        try {
            auditLogMapper.insert(row);
        } catch (RuntimeException ignored) {
            // Cancellation requests must not be blocked by audit storage availability.
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private List<BiQueryColumn> columns(BiDatasetSpec dataset, BiQueryRequest request) {
        return java.util.stream.Stream.concat(
                        request.dimensions().stream()
                                .map(key -> {
                                    BiFieldSpec field = dataset.fields().get(key);
                                    return new BiQueryColumn(key, field.role().name(), field.valueType());
                                }),
                        request.metrics().stream()
                                .map(key -> new BiQueryColumn(key, "METRIC", dataset.metrics().get(key).valueType())))
                .toList();
    }

    private String hash(BiCompiledQuery query) {
        return hash(query, "bi-permission:none");
    }

    private String hash(BiCompiledQuery query, String permissionSignature) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(query.sql().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(query.parameters().toString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(String.valueOf(permissionSignature).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String requestHash(BiQueryRequest request) {
        return requestHash("BI_REQUEST", request);
    }

    private String requestHash(String reason, BiQueryRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(reason.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(String.valueOf(request).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public record GatedBiQueryResult(
            Long tenantId,
            String datasetKey,
            String status,
            String reason,
            CdpWarehouseAvailabilityService.AvailabilityDecision availability,
            BiQueryResult queryResult) {
    }

    public record ContractGatedBiQueryResult(
            Long tenantId,
            String datasetKey,
            String contractKey,
            String status,
            String reason,
            CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation consumerAvailability,
            BiQueryResult queryResult) {
    }
}
