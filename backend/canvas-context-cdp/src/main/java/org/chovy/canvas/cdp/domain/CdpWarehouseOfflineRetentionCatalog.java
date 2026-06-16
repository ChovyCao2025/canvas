package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.List;

import org.chovy.canvas.cdp.api.CdpWarehouseOfflineRetentionFacade.OfflineCyclePlanView;
import org.chovy.canvas.cdp.api.CdpWarehouseOfflineRetentionFacade.OfflineCycleResultView;
import org.chovy.canvas.cdp.api.CdpWarehouseOfflineRetentionFacade.OfflineCycleStepPlanView;
import org.chovy.canvas.cdp.api.CdpWarehouseOfflineRetentionFacade.OfflineCycleStepResultView;
import org.chovy.canvas.cdp.api.CdpWarehouseOfflineRetentionFacade.RetentionCleanupResultView;
import org.chovy.canvas.cdp.api.CdpWarehouseOfflineRetentionFacade.RetentionPlanView;
import org.chovy.canvas.cdp.api.CdpWarehouseOfflineRetentionFacade.RetentionTargetPlanView;
import org.chovy.canvas.cdp.api.CdpWarehouseOfflineRetentionFacade.RetentionTargetResultView;

/**
 * 维护 CdpWarehouseOfflineRetention 的内存目录和查询视图。
 */
public class CdpWarehouseOfflineRetentionCatalog {

    /**
     * MAX BACKFILL LIMIT。
     */
    private static final int MAX_BACKFILL_LIMIT = 5000;

    /**
     * MAX RETENTION DAYS。
     */
    private static final int MAX_RETENTION_DAYS = 3650;

    /**
     * DEFAULT OPERATOR。
     */
    private static final String DEFAULT_OPERATOR = "warehouse-retention";

    /**
     * STATUS READY。
     */
    private static final String STATUS_READY = "READY";

    /**
     * STATUS WAITING。
     */
    private static final String STATUS_WAITING = "WAITING_FOR_BACKFILL";

    /**
     * STATUS SUCCESS。
     */
    private static final String STATUS_SUCCESS = "SUCCESS";

    /**
     * 执行 offlineCyclePlan 对应的 CDP 业务操作。
     */
    public OfflineCyclePlanView offlineCyclePlan(Long tenantId, LocalDateTime now, int backfillLimit,
                                                 int aggregationWindowMinutes) {
        requireBackfillLimit(backfillLimit);
        if (aggregationWindowMinutes <= 0) {
            throw new IllegalArgumentException("aggregationWindowMinutes must be positive");
        }
        LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
        LocalDateTime windowEnd = effectiveNow.withSecond(0).withNano(0);
        LocalDateTime windowStart = windowEnd.minusMinutes(aggregationWindowMinutes);
        return new OfflineCyclePlanView(tenantId, effectiveNow, backfillLimit, aggregationWindowMinutes, List.of(
                new OfflineCycleStepPlanView("BACKFILL", STATUS_READY,
                        "ready to replay accepted CDP events after id 0", 0L, null, null, null),
                new OfflineCycleStepPlanView("AGGREGATE", STATUS_WAITING,
                        "will run after backfill succeeds", null, null, windowStart, windowEnd)));
    }

    /**
     * 执行 runOfflineCycle 对应的 CDP 业务操作。
     */
    public OfflineCycleResultView runOfflineCycle(Long tenantId, LocalDateTime now, int backfillLimit,
                                                  int aggregationWindowMinutes, String operator) {
        OfflineCyclePlanView plan = offlineCyclePlan(tenantId, now, backfillLimit, aggregationWindowMinutes);
        String actor = normalizeOperator(operator, "warehouse-scheduler");
        long loaded = Math.max(1, Math.min(backfillLimit, 1000));
        long aggregated = aggregationWindowMinutes * 2L;
        return new OfflineCycleResultView(tenantId, plan.plannedAt(), actor, STATUS_SUCCESS, loaded + aggregated, 0L,
                loaded, null, List.of(
                new OfflineCycleStepResultView("BACKFILL", STATUS_SUCCESS, loaded, 0L, loaded, null, null,
                        STATUS_SUCCESS),
                new OfflineCycleStepResultView("AGGREGATE", STATUS_SUCCESS, aggregated, 0L, null,
                        plan.steps().get(1).windowStart(), plan.steps().get(1).windowEnd(), STATUS_SUCCESS)));
    }

    /**
     * 执行 retentionPlan 对应的 CDP 业务操作。
     */
    public RetentionPlanView retentionPlan(Long tenantId, LocalDateTime now, int syncRunRetentionDays,
                                           int realtimeRetryRetentionDays, int resolvedIncidentRetentionDays) {
        LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
        RetentionTargetPlanView syncRuns = targetPlan("SYNC_RUNS", syncRunRetentionDays, effectiveNow, 2L,
                "finished sync runs older than cutoff");
        RetentionTargetPlanView retries = targetPlan("REALTIME_RETRIES", realtimeRetryRetentionDays, effectiveNow, 3L,
                "terminal realtime retry rows older than cutoff");
        RetentionTargetPlanView incidents = targetPlan("RESOLVED_INCIDENTS", resolvedIncidentRetentionDays,
                effectiveNow, 1L, "resolved incidents older than cutoff");
        return new RetentionPlanView(tenantId, effectiveNow, syncRuns, retries, incidents,
                syncRuns.eligibleRows() + retries.eligibleRows() + incidents.eligibleRows());
    }

    /**
     * 执行 runRetention 对应的 CDP 业务操作。
     */
    public RetentionCleanupResultView runRetention(Long tenantId, LocalDateTime now, int syncRunRetentionDays,
                                                   int realtimeRetryRetentionDays, int resolvedIncidentRetentionDays,
                                                   String operator) {
        RetentionPlanView plan = retentionPlan(tenantId, now, syncRunRetentionDays, realtimeRetryRetentionDays,
                resolvedIncidentRetentionDays);
        RetentionTargetResultView syncRuns = targetResult(plan.syncRuns());
        RetentionTargetResultView retries = targetResult(plan.realtimeRetries());
        RetentionTargetResultView incidents = targetResult(plan.resolvedIncidents());
        return new RetentionCleanupResultView(tenantId, plan.generatedAt(), normalizeOperator(operator, DEFAULT_OPERATOR),
                syncRuns, retries, incidents,
                (long) syncRuns.deletedRows() + retries.deletedRows() + incidents.deletedRows());
    }

    /**
     * 执行 targetPlan 对应的 CDP 业务操作。
     */
    private static RetentionTargetPlanView targetPlan(String targetKey, int days, LocalDateTime now, long eligibleRows,
                                                      String rule) {
        requireRetentionDays(days, targetKey);
        return new RetentionTargetPlanView(targetKey, days, now.minusDays(days), eligibleRows, rule);
    }

    /**
     * 执行 targetResult 对应的 CDP 业务操作。
     */
    private static RetentionTargetResultView targetResult(RetentionTargetPlanView plan) {
        return new RetentionTargetResultView(plan.targetKey(), plan.retentionDays(), plan.cutoff(),
                plan.eligibleRows(), Math.toIntExact(plan.eligibleRows()));
    }

    /**
     * 读取并校验必填的Backfill Limit。
     */
    private static void requireBackfillLimit(int limit) {
        if (limit <= 0 || limit > MAX_BACKFILL_LIMIT) {
            throw new IllegalArgumentException("backfillLimit must be between 1 and " + MAX_BACKFILL_LIMIT);
        }
    }

    /**
     * 读取并校验必填的Retention Days。
     */
    private static void requireRetentionDays(int days, String targetKey) {
        if (days <= 0 || days > MAX_RETENTION_DAYS) {
            String name = switch (targetKey) {
                case "SYNC_RUNS" -> "syncRunRetentionDays";
                case "REALTIME_RETRIES" -> "realtimeRetryRetentionDays";
                default -> "resolvedIncidentRetentionDays";
            };
            throw new IllegalArgumentException(name + " must be between 1 and " + MAX_RETENTION_DAYS);
        }
    }

    /**
     * 归一化Operator。
     */
    private static String normalizeOperator(String operator, String fallback) {
        return operator == null || operator.isBlank() ? fallback : operator.trim();
    }
}
