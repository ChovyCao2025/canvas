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

/**
 * BiDatasetAccelerationService 编排 domain.bi.dataset 场景的领域业务规则。
 */
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

    /**
     * 创建 BiDatasetAccelerationService 实例并注入 domain.bi.dataset 场景依赖。
     * @param policyMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param materializer materializer 参数，用于 BiDatasetAccelerationService 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 创建 BiDatasetAccelerationService 实例并注入 domain.bi.dataset 场景依赖。
     * @param policyMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param materializer materializer 参数，用于 BiDatasetAccelerationService 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param extractRetainedTables extract retained tables 参数，用于 BiDatasetAccelerationService 流程中的校验、计算或对象转换。
     */
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

    /**
     * 创建 BiDatasetAccelerationService 实例并注入 domain.bi.dataset 场景依赖。
     * @param policyMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetSpecResolverProvider 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param materializerProvider materializer provider 参数，用于 BiDatasetAccelerationService 流程中的校验、计算或对象转换。
     * @param extractRetainedTables extract retained tables 参数，用于 BiDatasetAccelerationService 流程中的校验、计算或对象转换。
     */
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

    /**
     * 读取数据集加速策略视图，用于展示抽取模式、刷新频率和容量占用配置。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param datasetKey 数据集业务键，用于定位语义模型、权限规则和加速策略
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiDatasetAccelerationPolicyView policyView(Long tenantId, String datasetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedDatasetKey = requiredDatasetKey(datasetKey);
        return view(scopedDatasetKey, findPolicy(scopedTenantId, scopedDatasetKey),
                loadRecentRuns(scopedTenantId, scopedDatasetKey, 10));
    }

    /**
     * 创建或更新治理策略，并记录操作者以支撑后续审计和运行时生效。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param datasetKey 数据集业务键，用于定位语义模型、权限规则和加速策略
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @param actor 触发策略变更、调度或刷新动作的操作者标识
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiDatasetAccelerationPolicyView upsertPolicy(Long tenantId,
                                                        String datasetKey,
                                                        BiDatasetAccelerationPolicyCommand command,
                                                        String actor) {
        // 准备本次流程的上下文、默认值和中间结果。
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
        // 访问持久化数据，读取现有配置或写入本次变更。
        row.setUpdatedBy(actor(actor));
        // 校验策略输入和默认值，避免无效配置进入持久化或查询流程。
        if (row.getId() == null) {
            policyMapper.insert(row);
        } else {
            policyMapper.updateById(row);
        }
        BiDatasetAccelerationPolicyView after = policyView(scopedTenantId, scopedDatasetKey);
        auditUpdate(scopedTenantId, actor(actor), scopedDatasetKey, before, after);
        return after;
    }

    /**
     * 立即触发数据集加速抽取刷新，物化最新数据并记录刷新运行结果。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param datasetKey 数据集业务键，用于定位语义模型、权限规则和加速策略
     * @param actor 触发策略变更、调度或刷新动作的操作者标识
     * @return 用于前端展示或管理端审计的业务视图
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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

    /**
     * 查询数据集加速最近刷新记录，用于观察抽取成功率、耗时和失败原因。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param datasetKey 数据集业务键，用于定位语义模型、权限规则和加速策略
     * @param limit 本次读取、处理或领取的最大数量
     * @return 最新的业务视图
     */
    public List<BiDatasetExtractRefreshRunView> recentRuns(Long tenantId, String datasetKey, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        return loadRecentRuns(scopedTenantId, requiredDatasetKey(datasetKey), limit);
    }

    /**
     * 汇总数据集抽取容量占用，用于评估加速缓存的存储压力和清理策略。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param datasetKey 数据集业务键，用于定位语义模型、权限规则和加速策略
     * @param limit 本次读取、处理或领取的最大数量
     * @return 用于前端展示或管理端审计的业务视图
     */
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
            // 根据前序判断结果进入后续条件分支。
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

    /**
     * 清理超过保留数量的历史抽取表，释放加速缓存容量并保留最近可回滚版本。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param datasetKey 数据集业务键，用于定位语义模型、权限规则和加速策略
     * @param retainTables 需要保留的历史抽取表数量
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiDatasetExtractCleanupResultView cleanupRetainedExtracts(Long tenantId, String datasetKey, int retainTables) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedDatasetKey = requiredDatasetKey(datasetKey);
        int retention = Math.max(1, Math.min(retainTables <= 0 ? extractRetainedTables : retainTables, 50));
        return cleanupRetainedExtractsInternal(scopedTenantId, scopedDatasetKey, retention);
    }

    /**
     * 按数据集加速策略改写查询语义，使查询优先命中已物化的 Quick Engine 抽取表。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param dataset 待查询或待加速的数据集语义定义
     * @return 按加速策略改写后的数据集语义定义
     */
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
                /**
                 * 判断业务条件是否成立。
                 *
                 * @return 返回布尔判断结果。
                 */
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

    /**
     * 执行数据写入或状态变更。
     *
     * @param policy policy 参数，用于 updatePolicyAfterRefresh 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param run run 参数，用于 updatePolicyAfterRefresh 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param errorSummary error summary 参数，用于 updatePolicyAfterRefresh 流程中的校验、计算或对象转换。
     */
    private void updatePolicyAfterRefresh(BiDatasetAccelerationPolicyDO policy,
                                          Long tenantId,
                                          String datasetKey,
                                          BiDatasetExtractRefreshRunDO run,
                                          String status,
                                          String errorSummary) {
        // 刷新运行表记录单次执行事实；策略表只保存最后一次状态和当前可查询的物化表指针。
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

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private BiDatasetAccelerationPolicyDO findPolicy(Long tenantId, String datasetKey) {
        List<BiDatasetAccelerationPolicyDO> rows = policyMapper.selectList(
                new LambdaQueryWrapper<BiDatasetAccelerationPolicyDO>()
                        .eq(BiDatasetAccelerationPolicyDO::getTenantId, tenantId)
                        .eq(BiDatasetAccelerationPolicyDO::getDatasetKey, datasetKey));
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 刷新完成后的保留清理是“尽力而为”的容量治理动作。
     *
     * <p>新抽取表已经写入策略并可被查询使用，历史表清理失败不应回滚刷新成功状态；失败的清理会留给后续人工
     * 或定时清理重试。</p>
     */
    private void cleanupAfterRefresh(Long tenantId, String datasetKey) {
        try {
            cleanupRetainedExtractsInternal(tenantId, datasetKey, extractRetainedTables);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ignored) {
            // A successful extract must remain usable even when old-table cleanup is temporarily unavailable.
        }
    }

    /**
     * 按刷新完成时间倒序清理超过保留数量的活动物化表。
     *
     * <p>同一个物化表可能对应多条成功刷新记录，因此以表名聚合后再删除；删除成功后将该表关联的刷新记录标记为
     * DROPPED，容量汇总和加速改写都不再把它计为活动版本。</p>
     */
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
                // 只有物理表删除成功后才改写 retention 状态，避免审计口径与实际存储不一致。
                if (materializer.dropMaterializedTable(entry.getKey())) {
                    droppedTables++;
                    markDropped(entry.getValue());
                } else {
                    failedDrops++;
                }
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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

    /**
     * 读取当前仍被容量治理视为活动的物化表集合。
     *
     * <p>查询只看成功刷新且未被标记 DROPPED 的运行记录，并保持数据库返回顺序以表达“最新优先”的保留口径。
     * 返回值按物化表名聚合，避免同表多次记录导致重复删除或重复计量。</p>
     */
    private Map<String, List<BiDatasetExtractRefreshRunDO>> activeMaterializedTables(Long tenantId, String datasetKey) {
        Map<String, List<BiDatasetExtractRefreshRunDO>> tables = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiDatasetExtractRefreshRunDO row : loadSuccessfulRuns(tenantId, datasetKey, 500)) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (RETENTION_DROPPED.equals(normalizeRetentionStatus(row.getRetentionStatus()))) {
                continue;
            }
            String table = trimToNull(row.getMaterializedTable());
            if (table == null) {
                continue;
            }
            tables.computeIfAbsent(table, ignored -> new java.util.ArrayList<>()).add(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return tables;
    }

    /**
     * 将一组刷新记录标记为已丢弃，作为容量视图和后续清理的统一事实来源。
     */
    private void markDropped(List<BiDatasetExtractRefreshRunDO> rows) {
        LocalDateTime droppedAt = now();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiDatasetExtractRefreshRunDO row : rows) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            BiDatasetExtractRefreshRunDO update = new BiDatasetExtractRefreshRunDO();
            update.setId(row.getId());
            update.setRetentionStatus(RETENTION_DROPPED);
            update.setDroppedAt(droppedAt);
            runMapper.updateById(update);
        }
    }

    /**
     * 执行 completionRun 流程，围绕 completion run 完成校验、计算或结果组装。
     *
     * @param insertedRun inserted run 参数，用于 completionRun 流程中的校验、计算或对象转换。
     * @return 返回 completionRun 流程生成的业务结果。
     */
    private BiDatasetExtractRefreshRunDO completionRun(BiDatasetExtractRefreshRunDO insertedRun) {
        // 准备本次处理所需的上下文和中间变量。
        BiDatasetExtractRefreshRunDO row = new BiDatasetExtractRefreshRunDO();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setId(insertedRun.getId());
        row.setTenantId(insertedRun.getTenantId());
        row.setDatasetKey(insertedRun.getDatasetKey());
        row.setRequestedBy(insertedRun.getRequestedBy());
        row.setStartedAt(insertedRun.getStartedAt());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return row;
    }

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 load recent runs 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 load raw runs 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 load successful runs 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param recentRuns recent runs 参数，用于 view 流程中的校验、计算或对象转换。
     * @return 返回 view 流程生成的业务结果。
     */
    private BiDatasetAccelerationPolicyView view(String datasetKey,
                                                 BiDatasetAccelerationPolicyDO row,
                                                 List<BiDatasetExtractRefreshRunView> recentRuns) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param before before 参数，用于 auditUpdate 流程中的校验、计算或对象转换。
     * @param after after 参数，用于 auditUpdate 流程中的校验、计算或对象转换。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ignored) {
            // Acceleration settings should still apply if audit persistence is unavailable.
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * 规范化输入值。
     *
     * @param mode mode 参数，用于 normalizeMode 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeMode(String mode) {
        String normalized = isBlank(mode) ? MODE_DIRECT_QUERY : mode.trim().toUpperCase(Locale.ROOT);
        if (!MODES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported BI dataset acceleration mode: " + normalized);
        }
        return normalized;
    }

    /**
     * 规范化输入值。
     *
     * @param mode mode 参数，用于 normalizeRefreshMode 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeRefreshMode(String mode) {
        String normalized = isBlank(mode) ? REFRESH_MANUAL : mode.trim().toUpperCase(Locale.ROOT);
        if (!REFRESH_MODES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported BI dataset refresh mode: " + normalized);
        }
        return normalized;
    }

    /**
     * 规范化输入值。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        return isBlank(status) ? STATUS_IDLE : status.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeRetentionStatus(String status) {
        return isBlank(status) ? RETENTION_ACTIVE : status.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 required dataset key 生成的文本或业务键。
     */
    private String requiredDatasetKey(String datasetKey) {
        return required(datasetKey, "datasetKey");
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 value 流程中的校验、计算或对象转换。
     * @return 返回 value 的布尔判断结果。
     */
    private boolean value(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 value 流程中的校验、计算或对象转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private long value(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 value 流程中的校验、计算或对象转换。
     * @return 返回 value 生成的文本或业务键。
     */
    private String value(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    /**
     * 执行 positive 流程，围绕 positive 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 positive 流程中的校验、计算或对象转换。
     * @return 返回 positive 计算得到的数量、金额或指标值。
     */
    private long positive(Long value, long fallback) {
        if (value == null || value <= 0) {
            return fallback;
        }
        return value;
    }

    /**
     * 执行 positive 流程，围绕 positive 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 positive 流程中的校验、计算或对象转换。
     * @return 返回 positive 计算得到的数量、金额或指标值。
     */
    private long positive(long value, long fallback) {
        return value <= 0 ? fallback : value;
    }

    /**
     * 解析操作人标识。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(String actor) {
        return isBlank(actor) ? "system" : actor.trim();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 执行 summarize 流程，围绕 summarize 完成校验、计算或结果组装。
     *
     * @param e e 参数，用于 summarize 流程中的校验、计算或对象转换。
     * @return 返回 summarize 生成的文本或业务键。
     */
    private String summarize(RuntimeException e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
