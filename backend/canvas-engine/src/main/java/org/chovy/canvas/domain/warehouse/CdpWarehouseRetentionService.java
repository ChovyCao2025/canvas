package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseIncidentDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseRealtimeRetryDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseSyncRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseIncidentMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseRealtimeRetryMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseSyncRunMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
/**
 * CdpWarehouseRetentionService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseRetentionService {

    private static final int MAX_RETENTION_DAYS = 3650;
    private static final String DEFAULT_OPERATOR = "warehouse-retention";
    private static final String TARGET_SYNC_RUNS = "SYNC_RUNS";
    private static final String TARGET_REALTIME_RETRIES = "REALTIME_RETRIES";
    private static final String TARGET_RESOLVED_INCIDENTS = "RESOLVED_INCIDENTS";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_DEAD = "DEAD";
    private static final String STATUS_RESOLVED = "RESOLVED";

    private final CdpWarehouseSyncRunMapper syncRunMapper;
    private final CdpWarehouseRealtimeRetryMapper retryMapper;
    private final CdpWarehouseIncidentMapper incidentMapper;

    /**
     * 初始化 CdpWarehouseRetentionService 实例。
     *
     * @param syncRunMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param retryMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param incidentMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseRetentionService(CdpWarehouseSyncRunMapper syncRunMapper,
                                        CdpWarehouseRealtimeRetryMapper retryMapper,
                                        CdpWarehouseIncidentMapper incidentMapper) {
        this.syncRunMapper = syncRunMapper;
        this.retryMapper = retryMapper;
        this.incidentMapper = incidentMapper;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param syncRunRetentionDays sync run retention days 参数，用于 plan 流程中的校验、计算或对象转换。
     * @param realtimeRetryRetentionDays 时间参数，用于计算窗口、过期或审计时间。
     * @param resolvedIncidentRetentionDays resolved incident retention days 参数，用于 plan 流程中的校验、计算或对象转换。
     * @return 返回 plan 流程生成的业务结果。
     */
    public RetentionPlan plan(
            Long tenantId,
            LocalDateTime now,
            int syncRunRetentionDays,
            int realtimeRetryRetentionDays,
            int resolvedIncidentRetentionDays) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
        int syncDays = requireRetentionDays(syncRunRetentionDays, "syncRunRetentionDays");
        int retryDays = requireRetentionDays(realtimeRetryRetentionDays, "realtimeRetryRetentionDays");
        int incidentDays = requireRetentionDays(resolvedIncidentRetentionDays, "resolvedIncidentRetentionDays");
        LocalDateTime syncCutoff = effectiveNow.minusDays(syncDays);
        LocalDateTime retryCutoff = effectiveNow.minusDays(retryDays);
        LocalDateTime incidentCutoff = effectiveNow.minusDays(incidentDays);

        RetentionTargetPlan syncRuns = new RetentionTargetPlan(
                TARGET_SYNC_RUNS,
                syncDays,
                syncCutoff,
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                count(syncRunMapper.selectCount(syncRunQuery(scopedTenantId, syncCutoff))),
                "finished sync runs older than cutoff");
        RetentionTargetPlan retries = new RetentionTargetPlan(
                TARGET_REALTIME_RETRIES,
                retryDays,
                retryCutoff,
                count(retryMapper.selectCount(retryQuery(scopedTenantId, retryCutoff))),
                "terminal realtime retry rows older than cutoff");
        RetentionTargetPlan incidents = new RetentionTargetPlan(
                TARGET_RESOLVED_INCIDENTS,
                incidentDays,
                incidentCutoff,
                count(incidentMapper.selectCount(resolvedIncidentQuery(scopedTenantId, incidentCutoff))),
                "resolved incidents older than cutoff");
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new RetentionPlan(
                scopedTenantId,
                effectiveNow,
                syncRuns,
                retries,
                incidents,
                syncRuns.eligibleRows() + retries.eligibleRows() + incidents.eligibleRows());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param syncRunRetentionDays sync run retention days 参数，用于 cleanup 流程中的校验、计算或对象转换。
     * @param realtimeRetryRetentionDays 时间参数，用于计算窗口、过期或审计时间。
     * @param resolvedIncidentRetentionDays resolved incident retention days 参数，用于 cleanup 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 cleanup 流程生成的业务结果。
     */
    public RetentionCleanupResult cleanup(
            Long tenantId,
            LocalDateTime now,
            int syncRunRetentionDays,
            int realtimeRetryRetentionDays,
            int resolvedIncidentRetentionDays,
            String operator) {
        // 准备本次处理所需的上下文和中间变量。
        RetentionPlan plan = plan(
                tenantId,
                now,
                syncRunRetentionDays,
                realtimeRetryRetentionDays,
                resolvedIncidentRetentionDays);
        Long scopedTenantId = plan.tenantId();
        RetentionTargetResult syncRuns = new RetentionTargetResult(
                TARGET_SYNC_RUNS,
                plan.syncRuns().retentionDays(),
                plan.syncRuns().cutoff(),
                plan.syncRuns().eligibleRows(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                syncRunMapper.delete(syncRunQuery(scopedTenantId, plan.syncRuns().cutoff())));
        RetentionTargetResult retries = new RetentionTargetResult(
                TARGET_REALTIME_RETRIES,
                plan.realtimeRetries().retentionDays(),
                plan.realtimeRetries().cutoff(),
                plan.realtimeRetries().eligibleRows(),
                retryMapper.delete(retryQuery(scopedTenantId, plan.realtimeRetries().cutoff())));
        RetentionTargetResult incidents = new RetentionTargetResult(
                TARGET_RESOLVED_INCIDENTS,
                plan.resolvedIncidents().retentionDays(),
                plan.resolvedIncidents().cutoff(),
                plan.resolvedIncidents().eligibleRows(),
                incidentMapper.delete(resolvedIncidentQuery(scopedTenantId, plan.resolvedIncidents().cutoff())));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new RetentionCleanupResult(
                scopedTenantId,
                plan.generatedAt(),
                normalizeOperator(operator),
                syncRuns,
                retries,
                incidents,
                (long) syncRuns.deletedRows() + retries.deletedRows() + incidents.deletedRows());
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param cutoff cutoff 参数，用于 syncRunQuery 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private LambdaQueryWrapper<CdpWarehouseSyncRunDO> syncRunQuery(Long tenantId, LocalDateTime cutoff) {
        return new LambdaQueryWrapper<CdpWarehouseSyncRunDO>()
                .eq(CdpWarehouseSyncRunDO::getTenantId, tenantId)
                .isNotNull(CdpWarehouseSyncRunDO::getFinishedAt)
                .lt(CdpWarehouseSyncRunDO::getFinishedAt, cutoff);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param cutoff cutoff 参数，用于 retryQuery 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private LambdaQueryWrapper<CdpWarehouseRealtimeRetryDO> retryQuery(Long tenantId, LocalDateTime cutoff) {
        return new LambdaQueryWrapper<CdpWarehouseRealtimeRetryDO>()
                .eq(CdpWarehouseRealtimeRetryDO::getTenantId, tenantId)
                .in(CdpWarehouseRealtimeRetryDO::getStatus, List.of(STATUS_SUCCESS, STATUS_DEAD))
                .isNotNull(CdpWarehouseRealtimeRetryDO::getFinishedAt)
                .lt(CdpWarehouseRealtimeRetryDO::getFinishedAt, cutoff);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param cutoff cutoff 参数，用于 resolvedIncidentQuery 流程中的校验、计算或对象转换。
     * @return 返回 resolvedIncidentQuery 流程生成的业务结果。
     */
    private LambdaQueryWrapper<CdpWarehouseIncidentDO> resolvedIncidentQuery(Long tenantId, LocalDateTime cutoff) {
        return new LambdaQueryWrapper<CdpWarehouseIncidentDO>()
                .eq(CdpWarehouseIncidentDO::getTenantId, tenantId)
                .eq(CdpWarehouseIncidentDO::getStatus, STATUS_RESOLVED)
                .isNotNull(CdpWarehouseIncidentDO::getResolvedAt)
                .lt(CdpWarehouseIncidentDO::getResolvedAt, cutoff);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param days days 参数，用于 requireRetentionDays 流程中的校验、计算或对象转换。
     * @param name 名称文本，用于展示或唯一性校验。
     * @return 返回 require retention days 计算得到的数量、金额或指标值。
     */
    private int requireRetentionDays(int days, String name) {
        if (days <= 0 || days > MAX_RETENTION_DAYS) {
            throw new IllegalArgumentException(name + " must be between 1 and " + MAX_RETENTION_DAYS);
        }
        return days;
    }

    /**
     * 统计符合条件的数据规模或状态数量。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回统计数量。
     */
    private long count(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            return DEFAULT_OPERATOR;
        }
        return operator.trim();
    }

    /**
     * RetentionPlan 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RetentionPlan(
            Long tenantId,
            LocalDateTime generatedAt,
            RetentionTargetPlan syncRuns,
            RetentionTargetPlan realtimeRetries,
            RetentionTargetPlan resolvedIncidents,
            long totalEligibleRows) {
    }

    /**
     * RetentionTargetPlan 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RetentionTargetPlan(
            String targetKey,
            int retentionDays,
            LocalDateTime cutoff,
            long eligibleRows,
            String rule) {
    }

    /**
     * RetentionCleanupResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RetentionCleanupResult(
            Long tenantId,
            LocalDateTime cleanedAt,
            String operator,
            RetentionTargetResult syncRuns,
            RetentionTargetResult realtimeRetries,
            RetentionTargetResult resolvedIncidents,
            long totalDeletedRows) {
    }

    /**
     * RetentionTargetResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RetentionTargetResult(
            String targetKey,
            int retentionDays,
            LocalDateTime cutoff,
            long eligibleRows,
            int deletedRows) {
    }
}
