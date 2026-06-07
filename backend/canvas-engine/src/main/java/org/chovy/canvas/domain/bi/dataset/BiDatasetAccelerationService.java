package org.chovy.canvas.domain.bi.dataset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiDatasetAccelerationPolicyDO;
import org.chovy.canvas.dal.dataobject.BiDatasetExtractRefreshRunDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiDatasetAccelerationPolicyMapper;
import org.chovy.canvas.dal.mapper.BiDatasetExtractRefreshRunMapper;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class BiDatasetAccelerationService {

    public static final String MODE_DIRECT_QUERY = "DIRECT_QUERY";
    public static final String MODE_CACHE = "CACHE";
    public static final String MODE_EXTRACT = "EXTRACT";
    public static final String REFRESH_MANUAL = "MANUAL";
    public static final String REFRESH_SCHEDULED = "SCHEDULED";
    private static final String STATUS_IDLE = "IDLE";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String RETENTION_ACTIVE = "ACTIVE";
    private static final String RETENTION_DROPPED = "DROPPED";
    private static final String AUDIT_ACTION = "BI_DATASET_ACCELERATION_POLICY_UPDATE";
    private static final String AUDIT_RESOURCE_TYPE = "BI_DATASET_ACCELERATION_POLICY";
    private static final Set<String> MODES = Set.of(MODE_DIRECT_QUERY, MODE_CACHE, MODE_EXTRACT);
    private static final Set<String> REFRESH_MODES = Set.of(REFRESH_MANUAL, REFRESH_SCHEDULED);

    private final BiDatasetAccelerationPolicyMapper policyMapper;
    private final BiDatasetExtractRefreshRunMapper runMapper;
    private final BiAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final BiDatasetSpecResolver datasetSpecResolver;
    private final BiDatasetExtractMaterializer materializer;
    private final Clock clock;
    private final int extractRetainedTables;

    public BiDatasetAccelerationService(BiDatasetAccelerationPolicyMapper policyMapper,
                                        BiDatasetExtractRefreshRunMapper runMapper,
                                        BiAuditLogMapper auditLogMapper,
                                        ObjectMapper objectMapper,
                                        BiDatasetSpecResolver datasetSpecResolver,
                                        BiDatasetExtractMaterializer materializer,
                                        Clock clock) {
        this(policyMapper,
                runMapper,
                auditLogMapper,
                objectMapper,
                datasetSpecResolver,
                materializer,
                clock,
                2);
    }

    public BiDatasetAccelerationService(BiDatasetAccelerationPolicyMapper policyMapper,
                                        BiDatasetExtractRefreshRunMapper runMapper,
                                        BiAuditLogMapper auditLogMapper,
                                        ObjectMapper objectMapper,
                                        BiDatasetSpecResolver datasetSpecResolver,
                                        BiDatasetExtractMaterializer materializer,
                                        Clock clock,
                                        int extractRetainedTables) {
        this.policyMapper = policyMapper;
        this.runMapper = runMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
        this.materializer = materializer == null ? BiDatasetExtractMaterializer.unavailable() : materializer;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.extractRetainedTables = Math.max(1, Math.min(extractRetainedTables <= 0 ? 2 : extractRetainedTables, 50));
    }

    @Autowired
    public BiDatasetAccelerationService(BiDatasetAccelerationPolicyMapper policyMapper,
                                        BiDatasetExtractRefreshRunMapper runMapper,
                                        BiAuditLogMapper auditLogMapper,
                                        ObjectMapper objectMapper,
                                        ObjectProvider<BiDatasetSpecResolver> datasetSpecResolverProvider,
                                        ObjectProvider<BiDatasetExtractMaterializer> materializerProvider,
                                        @Value("${canvas.bi.dataset.acceleration.extract.retained-tables:2}") int extractRetainedTables) {
        this(policyMapper,
                runMapper,
                auditLogMapper,
                objectMapper,
                datasetSpecResolverProvider == null
                        ? BiDatasetSpecResolver.builtIn()
                        : datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn),
                materializerProvider == null
                        ? BiDatasetExtractMaterializer.unavailable()
                        : materializerProvider.getIfAvailable(BiDatasetExtractMaterializer::unavailable),
                Clock.systemUTC(),
                extractRetainedTables);
    }

    public BiDatasetAccelerationPolicyView policyView(Long tenantId, String datasetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedDatasetKey = requiredDatasetKey(datasetKey);
        return view(scopedDatasetKey, findPolicy(scopedTenantId, scopedDatasetKey),
                loadRecentRuns(scopedTenantId, scopedDatasetKey, 10));
    }

    public BiDatasetAccelerationPolicyView upsertPolicy(Long tenantId,
                                                        String datasetKey,
                                                        BiDatasetAccelerationPolicyCommand command,
                                                        String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedDatasetKey = requiredDatasetKey(datasetKey);
        BiDatasetAccelerationPolicyDO existing = findPolicy(scopedTenantId, scopedDatasetKey);
        BiDatasetAccelerationPolicyView before =
                view(scopedDatasetKey, existing, loadRecentRuns(scopedTenantId, scopedDatasetKey, 10));
        BiDatasetAccelerationPolicyCommand safeCommand = command == null
                ? new BiDatasetAccelerationPolicyCommand(null, null, null, null, null, null, null)
                : command;
        BiDatasetAccelerationPolicyDO row = existing == null ? new BiDatasetAccelerationPolicyDO() : existing;
        row.setTenantId(scopedTenantId);
        row.setDatasetKey(scopedDatasetKey);
        row.setEnabled(value(safeCommand.enabled(), before.enabled()));
        row.setAccelerationMode(normalizeMode(value(safeCommand.accelerationMode(), before.accelerationMode())));
        row.setRefreshMode(normalizeRefreshMode(value(safeCommand.refreshMode(), before.refreshMode())));
        row.setRefreshIntervalMinutes(positive(value(safeCommand.refreshIntervalMinutes(), before.refreshIntervalMinutes()), 60L));
        row.setTtlSeconds(positive(value(safeCommand.ttlSeconds(), before.ttlSeconds()), 300L));
        row.setMaxRows(positive(value(safeCommand.maxRows(), before.maxRows()), 100_000L));
        row.setCronExpression(trimToNull(value(safeCommand.cronExpression(), before.cronExpression())));
        row.setUpdatedBy(actor(actor));
        if (row.getId() == null) {
            policyMapper.insert(row);
        } else {
            policyMapper.updateById(row);
        }
        BiDatasetAccelerationPolicyView after = policyView(scopedTenantId, scopedDatasetKey);
        auditUpdate(scopedTenantId, actor(actor), scopedDatasetKey, before, after);
        return after;
    }

    public BiDatasetExtractRefreshRunView refreshNow(Long tenantId, String datasetKey, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedDatasetKey = requiredDatasetKey(datasetKey);
        BiDatasetAccelerationPolicyDO policy = findPolicy(scopedTenantId, scopedDatasetKey);
        BiDatasetAccelerationPolicyView policyView =
                view(scopedDatasetKey, policy, loadRecentRuns(scopedTenantId, scopedDatasetKey, 5));
        if (!policyView.enabled() || !MODE_EXTRACT.equals(policyView.accelerationMode())) {
            throw new IllegalStateException("BI dataset acceleration refresh requires enabled EXTRACT mode");
        }
        LocalDateTime startedAt = now();
        BiDatasetExtractRefreshRunDO run = new BiDatasetExtractRefreshRunDO();
        run.setTenantId(scopedTenantId);
        run.setDatasetKey(scopedDatasetKey);
        run.setStatus(STATUS_RUNNING);
        run.setRequestedBy(actor(actor));
        run.setStartedAt(startedAt);
        runMapper.insert(run);
        try {
            BiDatasetSpec dataset = datasetSpecResolver.dataset(scopedDatasetKey, scopedTenantId);
            BiDatasetExtractMaterializationResult result =
                    materializer.materialize(scopedTenantId, dataset, policyView);
            LocalDateTime finishedAt = now();
            BiDatasetExtractRefreshRunDO completedRun = completionRun(run);
            completedRun.setStatus(STATUS_SUCCESS);
            completedRun.setMaterializedTable(required(result.materializedTable(), "materializedTable"));
            completedRun.setRowCount(Math.max(0L, result.rowCount()));
            completedRun.setDurationMs(Math.max(0L, result.durationMs()));
            completedRun.setFinishedAt(finishedAt);
            completedRun.setRetentionStatus(RETENTION_ACTIVE);
            runMapper.updateById(completedRun);
            updatePolicyAfterRefresh(policy, scopedTenantId, scopedDatasetKey, completedRun, STATUS_SUCCESS, null);
            cleanupAfterRefresh(scopedTenantId, scopedDatasetKey);
            return toRunView(completedRun);
        } catch (RuntimeException e) {
            LocalDateTime finishedAt = now();
            BiDatasetExtractRefreshRunDO failedRun = completionRun(run);
            failedRun.setStatus(STATUS_FAILED);
            failedRun.setErrorMessage(summarize(e));
            failedRun.setFinishedAt(finishedAt);
            runMapper.updateById(failedRun);
            updatePolicyAfterRefresh(policy, scopedTenantId, scopedDatasetKey, failedRun, STATUS_FAILED, failedRun.getErrorMessage());
            throw e;
        }
    }

    public List<BiDatasetExtractRefreshRunView> recentRuns(Long tenantId, String datasetKey, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        return loadRecentRuns(scopedTenantId, requiredDatasetKey(datasetKey), limit);
    }

    public BiDatasetExtractCapacitySummaryView capacitySummary(Long tenantId, String datasetKey, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedDatasetKey = requiredDatasetKey(datasetKey);
        BiDatasetAccelerationPolicyView policy =
                view(scopedDatasetKey, findPolicy(scopedTenantId, scopedDatasetKey), List.of());
        List<BiDatasetExtractRefreshRunDO> rows = loadRawRuns(scopedTenantId, scopedDatasetKey, limit);
        int successfulRuns = 0;
        int failedRuns = 0;
        long retainedRows = 0L;
        Long latestRowCount = null;
        Long latestDurationMs = null;
        Map<String, Boolean> activeTables = new LinkedHashMap<>();
        Map<String, Boolean> droppedTables = new LinkedHashMap<>();
        for (BiDatasetExtractRefreshRunDO row : rows) {
            String status = normalizeStatus(row.getStatus());
            if (STATUS_SUCCESS.equals(status)) {
                successfulRuns++;
                if (latestRowCount == null) {
                    latestRowCount = row.getRowCount();
                    latestDurationMs = row.getDurationMs();
                }
                String table = trimToNull(row.getMaterializedTable());
                if (table != null) {
                    if (RETENTION_DROPPED.equals(normalizeRetentionStatus(row.getRetentionStatus()))) {
                        droppedTables.putIfAbsent(table, true);
                    } else {
                        activeTables.putIfAbsent(table, true);
                        retainedRows += Math.max(0L, row.getRowCount() == null ? 0L : row.getRowCount());
                    }
                }
            } else if (STATUS_FAILED.equals(status)) {
                failedRuns++;
            }
        }
        int staleTables = Math.max(0, activeTables.size() - extractRetainedTables);
        return new BiDatasetExtractCapacitySummaryView(
                scopedDatasetKey,
                policy.enabled(),
                policy.accelerationMode(),
                policy.refreshMode(),
                policy.materializedTable(),
                policy.lastStatus(),
                policy.lastRefreshedAt(),
                extractRetainedTables,
                successfulRuns,
                failedRuns,
                activeTables.size(),
                droppedTables.size(),
                staleTables,
                retainedRows,
                latestRowCount,
                latestDurationMs);
    }

    public BiDatasetExtractCleanupResultView cleanupRetainedExtracts(Long tenantId, String datasetKey, int retainTables) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedDatasetKey = requiredDatasetKey(datasetKey);
        int retention = Math.max(1, Math.min(retainTables <= 0 ? extractRetainedTables : retainTables, 50));
        return cleanupRetainedExtractsInternal(scopedTenantId, scopedDatasetKey, retention);
    }

    public BiDatasetSpec applyAcceleration(Long tenantId, BiDatasetSpec dataset) {
        if (dataset == null) {
            return null;
        }
        BiDatasetAccelerationPolicyDO policy = findPolicy(normalizeTenant(tenantId), dataset.datasetKey());
        if (policy == null
                || policy.getEnabled() == null
                || !policy.getEnabled()
                || !MODE_EXTRACT.equals(normalizeMode(policy.getAccelerationMode()))
                || !STATUS_SUCCESS.equals(normalizeStatus(policy.getLastStatus()))
                || isBlank(policy.getMaterializedTable())) {
            return dataset;
        }
        return new BiDatasetSpec(
                dataset.datasetKey(),
                policy.getMaterializedTable().trim(),
                dataset.tenantColumn(),
                dataset.fields(),
                dataset.metrics(),
                dataset.sqlParameters(),
                dataset.model());
    }

    private void updatePolicyAfterRefresh(BiDatasetAccelerationPolicyDO policy,
                                          Long tenantId,
                                          String datasetKey,
                                          BiDatasetExtractRefreshRunDO run,
                                          String status,
                                          String errorSummary) {
        BiDatasetAccelerationPolicyDO row = policy == null ? new BiDatasetAccelerationPolicyDO() : policy;
        row.setTenantId(tenantId);
        row.setDatasetKey(datasetKey);
        if (row.getEnabled() == null) {
            row.setEnabled(true);
        }
        if (isBlank(row.getAccelerationMode())) {
            row.setAccelerationMode(MODE_EXTRACT);
        }
        if (isBlank(row.getRefreshMode())) {
            row.setRefreshMode(REFRESH_MANUAL);
        }
        if (row.getRefreshIntervalMinutes() == null) {
            row.setRefreshIntervalMinutes(60L);
        }
        if (row.getTtlSeconds() == null) {
            row.setTtlSeconds(300L);
        }
        if (row.getMaxRows() == null) {
            row.setMaxRows(100_000L);
        }
        row.setLastStatus(status);
        row.setLastRunId(run.getId());
        if (STATUS_SUCCESS.equals(status)) {
            row.setMaterializedTable(run.getMaterializedTable());
            row.setLastRefreshedAt(run.getFinishedAt());
        }
        if (errorSummary != null) {
            row.setLastStatus(STATUS_FAILED);
        }
        row.setUpdatedBy(run.getRequestedBy());
        if (row.getId() == null) {
            policyMapper.insert(row);
        } else {
            policyMapper.updateById(row);
        }
    }

    private BiDatasetAccelerationPolicyDO findPolicy(Long tenantId, String datasetKey) {
        List<BiDatasetAccelerationPolicyDO> rows = policyMapper.selectList(
                new LambdaQueryWrapper<BiDatasetAccelerationPolicyDO>()
                        .eq(BiDatasetAccelerationPolicyDO::getTenantId, tenantId)
                        .eq(BiDatasetAccelerationPolicyDO::getDatasetKey, datasetKey));
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private void cleanupAfterRefresh(Long tenantId, String datasetKey) {
        try {
            cleanupRetainedExtractsInternal(tenantId, datasetKey, extractRetainedTables);
        } catch (RuntimeException ignored) {
            // A successful extract must remain usable even when old-table cleanup is temporarily unavailable.
        }
    }

    private BiDatasetExtractCleanupResultView cleanupRetainedExtractsInternal(Long tenantId, String datasetKey, int retention) {
        Map<String, List<BiDatasetExtractRefreshRunDO>> activeTables = activeMaterializedTables(tenantId, datasetKey);
        int checkedTables = activeTables.size();
        int retainedTables = 0;
        int droppedTables = 0;
        int failedDrops = 0;
        int index = 0;
        for (Map.Entry<String, List<BiDatasetExtractRefreshRunDO>> entry : activeTables.entrySet()) {
            if (index++ < retention) {
                retainedTables++;
                continue;
            }
            try {
                if (materializer.dropMaterializedTable(entry.getKey())) {
                    droppedTables++;
                    markDropped(entry.getValue());
                } else {
                    failedDrops++;
                }
            } catch (RuntimeException e) {
                failedDrops++;
            }
        }
        return new BiDatasetExtractCleanupResultView(
                datasetKey,
                checkedTables,
                retainedTables,
                droppedTables,
                failedDrops);
    }

    private Map<String, List<BiDatasetExtractRefreshRunDO>> activeMaterializedTables(Long tenantId, String datasetKey) {
        Map<String, List<BiDatasetExtractRefreshRunDO>> tables = new LinkedHashMap<>();
        for (BiDatasetExtractRefreshRunDO row : loadSuccessfulRuns(tenantId, datasetKey, 500)) {
            if (RETENTION_DROPPED.equals(normalizeRetentionStatus(row.getRetentionStatus()))) {
                continue;
            }
            String table = trimToNull(row.getMaterializedTable());
            if (table == null) {
                continue;
            }
            tables.computeIfAbsent(table, ignored -> new java.util.ArrayList<>()).add(row);
        }
        return tables;
    }

    private void markDropped(List<BiDatasetExtractRefreshRunDO> rows) {
        LocalDateTime droppedAt = now();
        for (BiDatasetExtractRefreshRunDO row : rows) {
            BiDatasetExtractRefreshRunDO update = new BiDatasetExtractRefreshRunDO();
            update.setId(row.getId());
            update.setRetentionStatus(RETENTION_DROPPED);
            update.setDroppedAt(droppedAt);
            runMapper.updateById(update);
        }
    }

    private BiDatasetExtractRefreshRunDO completionRun(BiDatasetExtractRefreshRunDO insertedRun) {
        BiDatasetExtractRefreshRunDO row = new BiDatasetExtractRefreshRunDO();
        row.setId(insertedRun.getId());
        row.setTenantId(insertedRun.getTenantId());
        row.setDatasetKey(insertedRun.getDatasetKey());
        row.setRequestedBy(insertedRun.getRequestedBy());
        row.setStartedAt(insertedRun.getStartedAt());
        return row;
    }

    private List<BiDatasetExtractRefreshRunView> loadRecentRuns(Long tenantId, String datasetKey, int limit) {
        int capped = Math.max(1, Math.min(limit <= 0 ? 10 : limit, 100));
        List<BiDatasetExtractRefreshRunDO> rows = runMapper.selectList(
                new LambdaQueryWrapper<BiDatasetExtractRefreshRunDO>()
                        .eq(BiDatasetExtractRefreshRunDO::getTenantId, tenantId)
                        .eq(BiDatasetExtractRefreshRunDO::getDatasetKey, datasetKey)
                        .orderByDesc(BiDatasetExtractRefreshRunDO::getStartedAt)
                        .last("LIMIT " + capped));
        return rows == null ? List.of() : rows.stream().map(this::toRunView).toList();
    }

    private List<BiDatasetExtractRefreshRunDO> loadRawRuns(Long tenantId, String datasetKey, int limit) {
        int capped = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 500));
        List<BiDatasetExtractRefreshRunDO> rows = runMapper.selectList(
                new LambdaQueryWrapper<BiDatasetExtractRefreshRunDO>()
                        .eq(BiDatasetExtractRefreshRunDO::getTenantId, tenantId)
                        .eq(BiDatasetExtractRefreshRunDO::getDatasetKey, datasetKey)
                        .orderByDesc(BiDatasetExtractRefreshRunDO::getFinishedAt)
                        .orderByDesc(BiDatasetExtractRefreshRunDO::getStartedAt)
                        .orderByDesc(BiDatasetExtractRefreshRunDO::getId)
                        .last("LIMIT " + capped));
        return rows == null ? List.of() : rows;
    }

    private List<BiDatasetExtractRefreshRunDO> loadSuccessfulRuns(Long tenantId, String datasetKey, int limit) {
        int capped = Math.max(1, Math.min(limit <= 0 ? 500 : limit, 500));
        List<BiDatasetExtractRefreshRunDO> rows = runMapper.selectList(
                new LambdaQueryWrapper<BiDatasetExtractRefreshRunDO>()
                        .eq(BiDatasetExtractRefreshRunDO::getTenantId, tenantId)
                        .eq(BiDatasetExtractRefreshRunDO::getDatasetKey, datasetKey)
                        .eq(BiDatasetExtractRefreshRunDO::getStatus, STATUS_SUCCESS)
                        .isNotNull(BiDatasetExtractRefreshRunDO::getMaterializedTable)
                        .orderByDesc(BiDatasetExtractRefreshRunDO::getFinishedAt)
                        .orderByDesc(BiDatasetExtractRefreshRunDO::getId)
                        .last("LIMIT " + capped));
        return rows == null ? List.of() : rows;
    }

    private BiDatasetAccelerationPolicyView view(String datasetKey,
                                                 BiDatasetAccelerationPolicyDO row,
                                                 List<BiDatasetExtractRefreshRunView> recentRuns) {
        if (row == null) {
            return new BiDatasetAccelerationPolicyView(
                    datasetKey,
                    false,
                    MODE_DIRECT_QUERY,
                    REFRESH_MANUAL,
                    60L,
                    300L,
                    100_000L,
                    null,
                    null,
                    STATUS_IDLE,
                    null,
                    null,
                    recentRuns);
        }
        return new BiDatasetAccelerationPolicyView(
                datasetKey,
                row.getEnabled() != null && row.getEnabled(),
                normalizeMode(row.getAccelerationMode()),
                normalizeRefreshMode(row.getRefreshMode()),
                positive(row.getRefreshIntervalMinutes(), 60L),
                positive(row.getTtlSeconds(), 300L),
                positive(row.getMaxRows(), 100_000L),
                row.getCronExpression(),
                row.getMaterializedTable(),
                normalizeStatus(row.getLastStatus()),
                row.getLastRunId(),
                row.getLastRefreshedAt(),
                recentRuns);
    }

    private BiDatasetExtractRefreshRunView toRunView(BiDatasetExtractRefreshRunDO row) {
        return new BiDatasetExtractRefreshRunView(
                row.getId(),
                row.getDatasetKey(),
                normalizeStatus(row.getStatus()),
                row.getRowCount(),
                row.getDurationMs(),
                row.getMaterializedTable(),
                row.getRequestedBy(),
                row.getStartedAt(),
                row.getFinishedAt(),
                row.getErrorMessage());
    }

    private void auditUpdate(Long tenantId,
                             String actor,
                             String datasetKey,
                             BiDatasetAccelerationPolicyView before,
                             BiDatasetAccelerationPolicyView after) {
        if (auditLogMapper == null) {
            return;
        }
        BiAuditLogDO row = new BiAuditLogDO();
        row.setTenantId(tenantId);
        row.setActorId(actor);
        row.setActionKey(AUDIT_ACTION);
        row.setResourceType(AUDIT_RESOURCE_TYPE);
        row.setDetailJson(toJson(Map.of("datasetKey", datasetKey, "before", before, "after", after)));
        row.setCreatedAt(now());
        try {
            auditLogMapper.insert(row);
        } catch (RuntimeException ignored) {
            // Acceleration settings should still apply if audit persistence is unavailable.
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String normalizeMode(String mode) {
        String normalized = isBlank(mode) ? MODE_DIRECT_QUERY : mode.trim().toUpperCase(Locale.ROOT);
        if (!MODES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported BI dataset acceleration mode: " + normalized);
        }
        return normalized;
    }

    private String normalizeRefreshMode(String mode) {
        String normalized = isBlank(mode) ? REFRESH_MANUAL : mode.trim().toUpperCase(Locale.ROOT);
        if (!REFRESH_MODES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported BI dataset refresh mode: " + normalized);
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        return isBlank(status) ? STATUS_IDLE : status.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRetentionStatus(String status) {
        return isBlank(status) ? RETENTION_ACTIVE : status.trim().toUpperCase(Locale.ROOT);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String requiredDatasetKey(String datasetKey) {
        return required(datasetKey, "datasetKey");
    }

    private String required(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private boolean value(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private long value(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    private String value(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private long positive(Long value, long fallback) {
        if (value == null || value <= 0) {
            return fallback;
        }
        return value;
    }

    private long positive(long value, long fallback) {
        return value <= 0 ? fallback : value;
    }

    private String actor(String actor) {
        return isBlank(actor) ? "system" : actor.trim();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String summarize(RuntimeException e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
