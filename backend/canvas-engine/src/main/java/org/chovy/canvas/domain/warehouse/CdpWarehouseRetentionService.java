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

    public CdpWarehouseRetentionService(CdpWarehouseSyncRunMapper syncRunMapper,
                                        CdpWarehouseRealtimeRetryMapper retryMapper,
                                        CdpWarehouseIncidentMapper incidentMapper) {
        this.syncRunMapper = syncRunMapper;
        this.retryMapper = retryMapper;
        this.incidentMapper = incidentMapper;
    }

    public RetentionPlan plan(
            Long tenantId,
            LocalDateTime now,
            int syncRunRetentionDays,
            int realtimeRetryRetentionDays,
            int resolvedIncidentRetentionDays) {
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
        return new RetentionPlan(
                scopedTenantId,
                effectiveNow,
                syncRuns,
                retries,
                incidents,
                syncRuns.eligibleRows() + retries.eligibleRows() + incidents.eligibleRows());
    }

    public RetentionCleanupResult cleanup(
            Long tenantId,
            LocalDateTime now,
            int syncRunRetentionDays,
            int realtimeRetryRetentionDays,
            int resolvedIncidentRetentionDays,
            String operator) {
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
        return new RetentionCleanupResult(
                scopedTenantId,
                plan.generatedAt(),
                normalizeOperator(operator),
                syncRuns,
                retries,
                incidents,
                (long) syncRuns.deletedRows() + retries.deletedRows() + incidents.deletedRows());
    }

    private LambdaQueryWrapper<CdpWarehouseSyncRunDO> syncRunQuery(Long tenantId, LocalDateTime cutoff) {
        return new LambdaQueryWrapper<CdpWarehouseSyncRunDO>()
                .eq(CdpWarehouseSyncRunDO::getTenantId, tenantId)
                .isNotNull(CdpWarehouseSyncRunDO::getFinishedAt)
                .lt(CdpWarehouseSyncRunDO::getFinishedAt, cutoff);
    }

    private LambdaQueryWrapper<CdpWarehouseRealtimeRetryDO> retryQuery(Long tenantId, LocalDateTime cutoff) {
        return new LambdaQueryWrapper<CdpWarehouseRealtimeRetryDO>()
                .eq(CdpWarehouseRealtimeRetryDO::getTenantId, tenantId)
                .in(CdpWarehouseRealtimeRetryDO::getStatus, List.of(STATUS_SUCCESS, STATUS_DEAD))
                .isNotNull(CdpWarehouseRealtimeRetryDO::getFinishedAt)
                .lt(CdpWarehouseRealtimeRetryDO::getFinishedAt, cutoff);
    }

    private LambdaQueryWrapper<CdpWarehouseIncidentDO> resolvedIncidentQuery(Long tenantId, LocalDateTime cutoff) {
        return new LambdaQueryWrapper<CdpWarehouseIncidentDO>()
                .eq(CdpWarehouseIncidentDO::getTenantId, tenantId)
                .eq(CdpWarehouseIncidentDO::getStatus, STATUS_RESOLVED)
                .isNotNull(CdpWarehouseIncidentDO::getResolvedAt)
                .lt(CdpWarehouseIncidentDO::getResolvedAt, cutoff);
    }

    private int requireRetentionDays(int days, String name) {
        if (days <= 0 || days > MAX_RETENTION_DAYS) {
            throw new IllegalArgumentException(name + " must be between 1 and " + MAX_RETENTION_DAYS);
        }
        return days;
    }

    private long count(Long value) {
        return value == null ? 0L : value;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            return DEFAULT_OPERATOR;
        }
        return operator.trim();
    }

    public record RetentionPlan(
            Long tenantId,
            LocalDateTime generatedAt,
            RetentionTargetPlan syncRuns,
            RetentionTargetPlan realtimeRetries,
            RetentionTargetPlan resolvedIncidents,
            long totalEligibleRows) {
    }

    public record RetentionTargetPlan(
            String targetKey,
            int retentionDays,
            LocalDateTime cutoff,
            long eligibleRows,
            String rule) {
    }

    public record RetentionCleanupResult(
            Long tenantId,
            LocalDateTime cleanedAt,
            String operator,
            RetentionTargetResult syncRuns,
            RetentionTargetResult realtimeRetries,
            RetentionTargetResult resolvedIncidents,
            long totalDeletedRows) {
    }

    public record RetentionTargetResult(
            String targetKey,
            int retentionDays,
            LocalDateTime cutoff,
            long eligibleRows,
            int deletedRows) {
    }
}
