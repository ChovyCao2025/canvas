package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseSyncRunDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseWatermarkDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseSyncRunMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseWatermarkMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CdpWarehouseOperationsService {

    private static final int MAX_BACKFILL_LIMIT = 5000;
    private static final int MAX_STATUS_LIMIT = 100;
    private static final String SCHEDULER_OPERATOR = "warehouse-scheduler";
    private static final String BACKFILL_JOB = "CDP_EVENT_BACKFILL";
    private static final String BACKFILL_WATERMARK = "LAST_EVENT_ID";
    private static final String AGGREGATE_JOB = "CDP_EVENT_AGGREGATE";
    private static final String AGGREGATE_WATERMARK = "WINDOW_END";
    private static final String OFFLINE_CYCLE_JOB = "OFFLINE_CYCLE";
    private static final String SOURCE_TABLE = "cdp_event_log";
    private static final String STATUS_READY = "READY";
    private static final String STATUS_WAITING = "WAITING_FOR_BACKFILL";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final int MAX_ERROR_LENGTH = 1000;

    private final CdpWarehouseBackfillService backfillService;
    private final CdpWarehouseAggregationService aggregationService;
    private final CdpWarehouseSyncRunMapper runMapper;
    private final CdpWarehouseWatermarkMapper watermarkMapper;

    public CdpWarehouseOperationsService(CdpWarehouseBackfillService backfillService,
                                         CdpWarehouseAggregationService aggregationService,
                                         CdpWarehouseSyncRunMapper runMapper,
                                         CdpWarehouseWatermarkMapper watermarkMapper) {
        this.backfillService = backfillService;
        this.aggregationService = aggregationService;
        this.runMapper = runMapper;
        this.watermarkMapper = watermarkMapper;
    }

    public WarehouseStatus status(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int boundedLimit = boundStatusLimit(limit);
        List<CdpWarehouseSyncRunDO> runs = runMapper.selectList(new LambdaQueryWrapper<CdpWarehouseSyncRunDO>()
                .eq(CdpWarehouseSyncRunDO::getTenantId, scopedTenantId)
                .orderByDesc(CdpWarehouseSyncRunDO::getStartedAt)
                .last("LIMIT " + boundedLimit));
        List<CdpWarehouseWatermarkDO> watermarks = watermarkMapper.selectList(
                new LambdaQueryWrapper<CdpWarehouseWatermarkDO>()
                        .eq(CdpWarehouseWatermarkDO::getTenantId, scopedTenantId)
                        .orderByAsc(CdpWarehouseWatermarkDO::getJobName)
                        .orderByAsc(CdpWarehouseWatermarkDO::getWatermarkType));
        return new WarehouseStatus(
                scopedTenantId,
                safeList(runs).stream().map(this::toRunRow).toList(),
                safeList(watermarks).stream().map(this::toWatermarkRow).toList());
    }

    public CdpWarehouseBackfillService.BackfillResult triggerBackfill(
            Long tenantId, Long lastId, int limit, String operator) {
        requireBackfillLimit(limit);
        return backfillService.backfill(normalizeTenant(tenantId), lastId, limit, normalizeOperator(operator));
    }

    public CdpWarehouseAggregationService.AggregationResult triggerAggregation(
            Long tenantId, LocalDateTime from, LocalDateTime to, String operator) {
        return aggregationService.aggregate(normalizeTenant(tenantId), from, to, normalizeOperator(operator));
    }

    public OfflineCyclePlan planOfflineCycle(
            Long tenantId, LocalDateTime now, int backfillLimit, int aggregationWindowMinutes) {
        requireBackfillLimit(backfillLimit);
        if (aggregationWindowMinutes <= 0) {
            throw new IllegalArgumentException("aggregationWindowMinutes must be positive");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
        long lastEventId = parseLongWatermark(findWatermark(scopedTenantId, BACKFILL_JOB, BACKFILL_WATERMARK), 0L);
        AggregationWindow aggregationWindow = nextAggregationWindow(scopedTenantId, effectiveNow, aggregationWindowMinutes);
        return new OfflineCyclePlan(
                scopedTenantId,
                effectiveNow,
                backfillLimit,
                aggregationWindowMinutes,
                List.of(
                        new OfflineCycleStepPlan(
                                "BACKFILL",
                                STATUS_READY,
                                "ready to replay accepted CDP events after id " + lastEventId,
                                lastEventId,
                                null,
                                null,
                                null),
                        aggregationWindow.due()
                                ? new OfflineCycleStepPlan(
                                "AGGREGATE",
                                STATUS_WAITING,
                                "will run after backfill succeeds",
                                null,
                                null,
                                aggregationWindow.from(),
                                aggregationWindow.to())
                                : new OfflineCycleStepPlan(
                                "AGGREGATE",
                                STATUS_SKIPPED,
                                "no aggregation window is due",
                                null,
                                null,
                                aggregationWindow.from(),
                                aggregationWindow.to())));
    }

    public OfflineCycleResult runOfflineCycle(
            Long tenantId,
            LocalDateTime now,
            int backfillLimit,
            int aggregationWindowMinutes,
            String operator) {
        OfflineCyclePlan plan = planOfflineCycle(tenantId, now, backfillLimit, aggregationWindowMinutes);
        Long scopedTenantId = plan.tenantId();
        OfflineCycleStepPlan backfillPlan = plan.steps().get(0);
        OfflineCycleStepPlan aggregationPlan = plan.steps().get(1);
        String actor = normalizeOperator(operator);
        CdpWarehouseSyncRunDO run = newOfflineCycleRun(scopedTenantId, backfillPlan, aggregationPlan, actor);
        runMapper.insert(run);

        CdpWarehouseBackfillService.BackfillResult backfillResult = null;
        CdpWarehouseAggregationService.AggregationResult aggregationResult = null;
        OfflineCycleStepResult backfillStep;
        OfflineCycleStepResult aggregationStep;
        String cycleStatus = STATUS_SUCCESS;
        String error = null;
        long loadedRows = 0L;
        long failedRows = 0L;
        Long sourceEndId = null;
        try {
            backfillResult = backfillService.backfill(
                    scopedTenantId, backfillPlan.sourceStartId(), backfillLimit, actor);
            loadedRows += backfillResult.loaded();
            failedRows += backfillResult.failed();
            sourceEndId = backfillResult.lastEventId();
            backfillStep = new OfflineCycleStepResult(
                    "BACKFILL",
                    backfillResult.status(),
                    backfillResult.loaded(),
                    backfillResult.failed(),
                    backfillResult.lastEventId(),
                    null,
                    null,
                    backfillResult.status());
            if (!STATUS_SUCCESS.equalsIgnoreCase(backfillResult.status())) {
                cycleStatus = STATUS_FAILED;
                error = "backfill failed with status " + backfillResult.status();
                aggregationStep = new OfflineCycleStepResult(
                        "AGGREGATE",
                        STATUS_BLOCKED,
                        0,
                        0,
                        null,
                        aggregationPlan.windowStart(),
                        aggregationPlan.windowEnd(),
                        "blocked because backfill did not succeed");
            } else if (STATUS_SKIPPED.equals(aggregationPlan.status())) {
                aggregationStep = new OfflineCycleStepResult(
                        "AGGREGATE",
                        STATUS_SKIPPED,
                        0,
                        0,
                        null,
                        aggregationPlan.windowStart(),
                        aggregationPlan.windowEnd(),
                        aggregationPlan.reason());
            } else {
                aggregationResult = aggregationService.aggregate(
                        scopedTenantId, aggregationPlan.windowStart(), aggregationPlan.windowEnd(), actor);
                long aggregationLoaded = (long) aggregationResult.dwdRows() + aggregationResult.dwsRows();
                loadedRows += aggregationLoaded;
                if (STATUS_FAILED.equalsIgnoreCase(aggregationResult.status())) {
                    failedRows += 1L;
                    cycleStatus = STATUS_FAILED;
                    error = "aggregation failed with status " + aggregationResult.status();
                }
                aggregationStep = new OfflineCycleStepResult(
                        "AGGREGATE",
                        aggregationResult.status(),
                        aggregationLoaded,
                        STATUS_FAILED.equalsIgnoreCase(aggregationResult.status()) ? 1 : 0,
                        null,
                        aggregationPlan.windowStart(),
                        aggregationPlan.windowEnd(),
                        aggregationResult.status());
            }
        } catch (RuntimeException e) {
            cycleStatus = STATUS_FAILED;
            error = limit(e.getMessage(), "offline cycle failed");
            backfillStep = backfillResult == null
                    ? new OfflineCycleStepResult(
                    "BACKFILL", STATUS_FAILED, 0, 1, null, null, null, error)
                    : new OfflineCycleStepResult(
                    "BACKFILL",
                    backfillResult.status(),
                    backfillResult.loaded(),
                    backfillResult.failed(),
                    backfillResult.lastEventId(),
                    null,
                    null,
                    backfillResult.status());
            aggregationStep = new OfflineCycleStepResult(
                    "AGGREGATE",
                    aggregationResult == null ? STATUS_BLOCKED : aggregationResult.status(),
                    aggregationResult == null ? 0 : (long) aggregationResult.dwdRows() + aggregationResult.dwsRows(),
                    aggregationResult == null ? 0 : STATUS_FAILED.equalsIgnoreCase(aggregationResult.status()) ? 1 : 0,
                    null,
                    aggregationPlan.windowStart(),
                    aggregationPlan.windowEnd(),
                    aggregationResult == null ? "blocked because cycle failed" : aggregationResult.status());
            failedRows = Math.max(failedRows, 1L);
        }

        run.setStatus(cycleStatus);
        run.setSourceEndId(sourceEndId);
        run.setLoadedRows(loadedRows);
        run.setFailedRows(failedRows);
        run.setErrorMessage(error);
        run.setFinishedAt(LocalDateTime.now());
        runMapper.updateById(run);
        return new OfflineCycleResult(
                run.getId(),
                scopedTenantId,
                cycleStatus,
                loadedRows,
                failedRows,
                error,
                List.of(backfillStep, aggregationStep));
    }

    public CdpWarehouseBackfillService.BackfillResult runIncrementalBackfill(Long tenantId, int limit) {
        requireBackfillLimit(limit);
        Long scopedTenantId = normalizeTenant(tenantId);
        long lastId = parseLongWatermark(findWatermark(scopedTenantId, BACKFILL_JOB, BACKFILL_WATERMARK), 0L);
        return backfillService.backfill(scopedTenantId, lastId, limit, SCHEDULER_OPERATOR);
    }

    public CdpWarehouseAggregationService.AggregationResult runIncrementalAggregation(
            Long tenantId, LocalDateTime now, int windowMinutes) {
        if (windowMinutes <= 0) {
            throw new IllegalArgumentException("windowMinutes must be positive");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
        LocalDateTime from = parseTimeWatermark(
                findWatermark(scopedTenantId, AGGREGATE_JOB, AGGREGATE_WATERMARK),
                effectiveNow.minusMinutes(windowMinutes));
        LocalDateTime to = from.plusMinutes(windowMinutes);
        if (to.isAfter(effectiveNow)) {
            to = effectiveNow;
        }
        if (!from.isBefore(to)) {
            return new CdpWarehouseAggregationService.AggregationResult("SKIPPED", 0, 0);
        }
        return aggregationService.aggregate(scopedTenantId, from, to, SCHEDULER_OPERATOR);
    }

    public OfflineCycleResult runScheduledOfflineCycle(Long tenantId, LocalDateTime now, int backfillLimit,
                                                       int aggregationWindowMinutes) {
        return runOfflineCycle(tenantId, now, backfillLimit, aggregationWindowMinutes, SCHEDULER_OPERATOR);
    }

    private CdpWarehouseWatermarkDO findWatermark(Long tenantId, String jobName, String type) {
        return watermarkMapper.selectOne(new LambdaQueryWrapper<CdpWarehouseWatermarkDO>()
                .eq(CdpWarehouseWatermarkDO::getTenantId, tenantId)
                .eq(CdpWarehouseWatermarkDO::getJobName, jobName)
                .eq(CdpWarehouseWatermarkDO::getWatermarkType, type)
                .last("LIMIT 1"));
    }

    private AggregationWindow nextAggregationWindow(Long tenantId, LocalDateTime now, int windowMinutes) {
        LocalDateTime from = parseTimeWatermark(
                findWatermark(tenantId, AGGREGATE_JOB, AGGREGATE_WATERMARK),
                now.minusMinutes(windowMinutes));
        LocalDateTime to = from.plusMinutes(windowMinutes);
        if (to.isAfter(now)) {
            to = now;
        }
        return new AggregationWindow(from, to, from.isBefore(to));
    }

    private CdpWarehouseSyncRunDO newOfflineCycleRun(
            Long tenantId,
            OfflineCycleStepPlan backfillPlan,
            OfflineCycleStepPlan aggregationPlan,
            String operator) {
        CdpWarehouseSyncRunDO run = new CdpWarehouseSyncRunDO();
        run.setTenantId(tenantId);
        run.setJobType(OFFLINE_CYCLE_JOB);
        run.setSourceTable(SOURCE_TABLE);
        run.setSourceStartId(backfillPlan.sourceStartId());
        run.setWindowStart(aggregationPlan.windowStart());
        run.setWindowEnd(aggregationPlan.windowEnd());
        run.setStatus(STATUS_RUNNING);
        run.setLoadedRows(0L);
        run.setFailedRows(0L);
        run.setStartedAt(LocalDateTime.now());
        run.setCreatedBy(operator);
        return run;
    }

    private RunRow toRunRow(CdpWarehouseSyncRunDO run) {
        return new RunRow(
                run.getId(),
                run.getJobType(),
                run.getSourceTable(),
                run.getSourceStartId(),
                run.getSourceEndId(),
                run.getWindowStart(),
                run.getWindowEnd(),
                run.getStatus(),
                nullToZero(run.getLoadedRows()),
                nullToZero(run.getFailedRows()),
                run.getErrorMessage(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getCreatedBy());
    }

    private WatermarkRow toWatermarkRow(CdpWarehouseWatermarkDO watermark) {
        return new WatermarkRow(
                watermark.getId(),
                watermark.getJobName(),
                watermark.getWatermarkType(),
                watermark.getWatermarkValue(),
                watermark.getWatermarkTime(),
                watermark.getUpdatedAt());
    }

    private int boundStatusLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_STATUS_LIMIT);
    }

    private void requireBackfillLimit(int limit) {
        if (limit <= 0 || limit > MAX_BACKFILL_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_BACKFILL_LIMIT);
        }
    }

    private long parseLongWatermark(CdpWarehouseWatermarkDO watermark, long defaultValue) {
        if (watermark == null || watermark.getWatermarkValue() == null) {
            return defaultValue;
        }
        try {
            long value = Long.parseLong(watermark.getWatermarkValue().trim());
            return Math.max(value, 0L);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private LocalDateTime parseTimeWatermark(CdpWarehouseWatermarkDO watermark, LocalDateTime defaultValue) {
        if (watermark == null || watermark.getWatermarkValue() == null) {
            return defaultValue;
        }
        try {
            return LocalDateTime.parse(watermark.getWatermarkValue().trim());
        } catch (RuntimeException ignored) {
            return defaultValue;
        }
    }

    private String normalizeOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            return "operator";
        }
        return operator;
    }

    private String limit(String message, String fallback) {
        String value = message == null || message.isBlank() ? fallback : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    public record WarehouseStatus(
            Long tenantId,
            List<RunRow> recentRuns,
            List<WatermarkRow> watermarks) {
    }

    public record RunRow(
            Long id,
            String jobType,
            String sourceTable,
            Long sourceStartId,
            Long sourceEndId,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            String status,
            long loadedRows,
            long failedRows,
            String errorMessage,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            String createdBy) {
    }

    public record WatermarkRow(
            Long id,
            String jobName,
            String watermarkType,
            String watermarkValue,
            LocalDateTime watermarkTime,
            LocalDateTime updatedAt) {
    }

    public record OfflineCyclePlan(
            Long tenantId,
            LocalDateTime generatedAt,
            int backfillLimit,
            int aggregationWindowMinutes,
            List<OfflineCycleStepPlan> steps) {
    }

    public record OfflineCycleStepPlan(
            String stepKey,
            String status,
            String reason,
            Long sourceStartId,
            Long sourceEndId,
            LocalDateTime windowStart,
            LocalDateTime windowEnd) {
    }

    public record OfflineCycleResult(
            Long runId,
            Long tenantId,
            String status,
            long loadedRows,
            long failedRows,
            String errorMessage,
            List<OfflineCycleStepResult> steps) {
    }

    public record OfflineCycleStepResult(
            String stepKey,
            String status,
            long loadedRows,
            long failedRows,
            Long lastEventId,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            String message) {
    }

    private record AggregationWindow(LocalDateTime from, LocalDateTime to, boolean due) {
    }
}
