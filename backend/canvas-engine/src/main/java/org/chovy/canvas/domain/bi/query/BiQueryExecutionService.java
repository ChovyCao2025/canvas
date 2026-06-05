package org.chovy.canvas.domain.bi.query;

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
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
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

    @Autowired
    public BiQueryExecutionService(BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   ObjectProvider<BiDatasetSpecResolver> datasetSpecResolverProvider,
                                   ObjectProvider<CdpWarehouseFieldGovernanceService> fieldGovernanceServiceProvider,
                                   ObjectProvider<BiPermissionService> permissionServiceProvider,
                                   ObjectProvider<CdpWarehouseAvailabilityService> availabilityServiceProvider,
                                   ObjectProvider<CdpWarehouseConsumerAvailabilityService>
                                           consumerAvailabilityServiceProvider) {
        this(new BiQueryCompiler(), executor, historyRecorder, resultCache,
                datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn),
                fieldGovernanceServiceProvider.getIfAvailable(),
                permissionServiceProvider.getIfAvailable(),
                availabilityServiceProvider == null ? null : availabilityServiceProvider.getIfAvailable(),
                consumerAvailabilityServiceProvider == null
                        ? null
                        : consumerAvailabilityServiceProvider.getIfAvailable(),
                Clock.systemUTC());
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
        BiDatasetSpec dataset = datasetSpecResolver.dataset(request.datasetKey(), context.tenantId());
        long startedAtMs = clock.instant().toEpochMilli();
        BiPermissionService.BiPreparedQuery prepared = prepareQuery(request, context, dataset, startedAtMs);
        BiQueryRequest scopedRequest = prepared.request();
        enforceFieldPolicy(scopedRequest, context, dataset, startedAtMs);
        BiCompiledQuery query = compiler.compile(dataset, scopedRequest, context.tenantId());
        String sqlHash = hash(query, prepared.permissionSignature());
        BiQueryResult cachedResult = resultCache.get(sqlHash)
                .map(result -> result.asCached(Math.max(0, clock.instant().toEpochMilli() - startedAtMs)))
                .orElse(null);
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
        try {
            List<Map<String, Object>> rows = executor.execute(query, dataset);
            List<Map<String, Object>> visibleRows = applyMasks(rows, prepared.columnMasks());
            long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
            BiQueryResult result = new BiQueryResult(
                    dataset.datasetKey(),
                    columns(dataset, scopedRequest),
                    visibleRows,
                    visibleRows.size(),
                    durationMs,
                    sqlHash);
            resultCache.put(sqlHash, result);
            recordHistory(new BiQueryHistoryEntry(
                    context.tenantId(),
                    context.username(),
                    scopedRequest,
                    sqlHash,
                    result.rowCount(),
                    durationMs,
                    "SUCCESS",
                    null));
            return result;
        } catch (RuntimeException e) {
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
        }
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

    private void recordHistory(BiQueryHistoryEntry entry) {
        try {
            historyRecorder.record(entry);
        } catch (RuntimeException ignored) {
            // Query history is audit metadata; a recorder outage must not break report reads.
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
