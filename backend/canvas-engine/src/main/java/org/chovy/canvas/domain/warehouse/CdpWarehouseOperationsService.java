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
/**
 * CdpWarehouseOperationsService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 CdpWarehouseOperationsService 实例。
     *
     * @param backfillService 依赖组件，用于完成数据访问或外部能力调用。
     * @param aggregationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param watermarkMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseOperationsService(CdpWarehouseBackfillService backfillService,
                                         CdpWarehouseAggregationService aggregationService,
                                         CdpWarehouseSyncRunMapper runMapper,
                                         CdpWarehouseWatermarkMapper watermarkMapper) {
        this.backfillService = backfillService;
        this.aggregationService = aggregationService;
        this.runMapper = runMapper;
        this.watermarkMapper = watermarkMapper;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 status 流程生成的业务结果。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param lastId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 triggerBackfill 流程生成的业务结果。
     */
    public CdpWarehouseBackfillService.BackfillResult triggerBackfill(
            Long tenantId, Long lastId, int limit, String operator) {
        requireBackfillLimit(limit);
        return backfillService.backfill(normalizeTenant(tenantId), lastId, limit, normalizeOperator(operator));
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 triggerAggregation 流程生成的业务结果。
     */
    public CdpWarehouseAggregationService.AggregationResult triggerAggregation(
            Long tenantId, LocalDateTime from, LocalDateTime to, String operator) {
        return aggregationService.aggregate(normalizeTenant(tenantId), from, to, normalizeOperator(operator));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param backfillLimit backfill limit 参数，用于 planOfflineCycle 流程中的校验、计算或对象转换。
     * @param aggregationWindowMinutes aggregation window minutes 参数，用于 planOfflineCycle 流程中的校验、计算或对象转换。
     * @return 返回 planOfflineCycle 流程生成的业务结果。
     */
    public OfflineCyclePlan planOfflineCycle(
            Long tenantId, LocalDateTime now, int backfillLimit, int aggregationWindowMinutes) {
        // 准备本次处理所需的上下文和中间变量。
        requireBackfillLimit(backfillLimit);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (aggregationWindowMinutes <= 0) {
            throw new IllegalArgumentException("aggregationWindowMinutes must be positive");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
        long lastEventId = parseLongWatermark(findWatermark(scopedTenantId, BACKFILL_JOB, BACKFILL_WATERMARK), 0L);
        AggregationWindow aggregationWindow = nextAggregationWindow(scopedTenantId, effectiveNow, aggregationWindowMinutes);
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param backfillLimit backfill limit 参数，用于 runOfflineCycle 流程中的校验、计算或对象转换。
     * @param aggregationWindowMinutes aggregation window minutes 参数，用于 runOfflineCycle 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    public OfflineCycleResult runOfflineCycle(
            Long tenantId,
            LocalDateTime now,
            int backfillLimit,
            int aggregationWindowMinutes,
            String operator) {
        // 准备本次处理所需的上下文和中间变量。
        OfflineCyclePlan plan = planOfflineCycle(tenantId, now, backfillLimit, aggregationWindowMinutes);
        Long scopedTenantId = plan.tenantId();
        OfflineCycleStepPlan backfillPlan = plan.steps().get(0);
        OfflineCycleStepPlan aggregationPlan = plan.steps().get(1);
        String actor = normalizeOperator(operator);
        CdpWarehouseSyncRunDO run = newOfflineCycleRun(scopedTenantId, backfillPlan, aggregationPlan, actor);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new OfflineCycleResult(
                run.getId(),
                scopedTenantId,
                cycleStatus,
                loadedRows,
                failedRows,
                error,
                List.of(backfillStep, aggregationStep));
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回流程执行后的业务结果。
     */
    public CdpWarehouseBackfillService.BackfillResult runIncrementalBackfill(Long tenantId, int limit) {
        requireBackfillLimit(limit);
        Long scopedTenantId = normalizeTenant(tenantId);
        long lastId = parseLongWatermark(findWatermark(scopedTenantId, BACKFILL_JOB, BACKFILL_WATERMARK), 0L);
        return backfillService.backfill(scopedTenantId, lastId, limit, SCHEDULER_OPERATOR);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param windowMinutes window minutes 参数，用于 runIncrementalAggregation 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public CdpWarehouseAggregationService.AggregationResult runIncrementalAggregation(
            Long tenantId, LocalDateTime now, int windowMinutes) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return aggregationService.aggregate(scopedTenantId, from, to, SCHEDULER_OPERATOR);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param backfillLimit backfill limit 参数，用于 runScheduledOfflineCycle 流程中的校验、计算或对象转换。
     * @param aggregationWindowMinutes aggregation window minutes 参数，用于 runScheduledOfflineCycle 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public OfflineCycleResult runScheduledOfflineCycle(Long tenantId, LocalDateTime now, int backfillLimit,
                                                       int aggregationWindowMinutes) {
        return runOfflineCycle(tenantId, now, backfillLimit, aggregationWindowMinutes, SCHEDULER_OPERATOR);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobName 名称文本，用于展示或唯一性校验。
     * @param type 类型标识，用于选择对应处理分支。
     * @return 返回符合条件的数据列表或视图。
     */
    private CdpWarehouseWatermarkDO findWatermark(Long tenantId, String jobName, String type) {
        return watermarkMapper.selectOne(new LambdaQueryWrapper<CdpWarehouseWatermarkDO>()
                .eq(CdpWarehouseWatermarkDO::getTenantId, tenantId)
                .eq(CdpWarehouseWatermarkDO::getJobName, jobName)
                .eq(CdpWarehouseWatermarkDO::getWatermarkType, type)
                .last("LIMIT 1"));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param windowMinutes window minutes 参数，用于 nextAggregationWindow 流程中的校验、计算或对象转换。
     * @return 返回 nextAggregationWindow 流程生成的业务结果。
     */
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

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param backfillPlan backfill plan 参数，用于 newOfflineCycleRun 流程中的校验、计算或对象转换。
     * @param aggregationPlan aggregation plan 参数，用于 newOfflineCycleRun 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 newOfflineCycleRun 流程生成的业务结果。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param run run 参数，用于 toRunRow 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param watermark watermark 参数，用于 toWatermarkRow 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private WatermarkRow toWatermarkRow(CdpWarehouseWatermarkDO watermark) {
        return new WatermarkRow(
                watermark.getId(),
                watermark.getJobName(),
                watermark.getWatermarkType(),
                watermark.getWatermarkValue(),
                watermark.getWatermarkTime(),
                watermark.getUpdatedAt());
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundStatusLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_STATUS_LIMIT);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     */
    private void requireBackfillLimit(int limit) {
        if (limit <= 0 || limit > MAX_BACKFILL_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_BACKFILL_LIMIT);
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param watermark watermark 参数，用于 parseLongWatermark 流程中的校验、计算或对象转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param watermark watermark 参数，用于 parseTimeWatermark 流程中的校验、计算或对象转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            return "operator";
        }
        return operator;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param fallback fallback 参数，用于 limit 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limit(String message, String fallback) {
        String value = message == null || message.isBlank() ? fallback : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to zero 计算得到的数量、金额或指标值。
     */
    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * WarehouseStatus 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record WarehouseStatus(
            Long tenantId,
            List<RunRow> recentRuns,
            List<WatermarkRow> watermarks) {
    }

    /**
     * RunRow 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * WatermarkRow 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record WatermarkRow(
            Long id,
            String jobName,
            String watermarkType,
            String watermarkValue,
            LocalDateTime watermarkTime,
            LocalDateTime updatedAt) {
    }

    /**
     * OfflineCyclePlan 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record OfflineCyclePlan(
            Long tenantId,
            LocalDateTime generatedAt,
            int backfillLimit,
            int aggregationWindowMinutes,
            List<OfflineCycleStepPlan> steps) {
    }

    /**
     * OfflineCycleStepPlan 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record OfflineCycleStepPlan(
            String stepKey,
            String status,
            String reason,
            Long sourceStartId,
            Long sourceEndId,
            LocalDateTime windowStart,
            LocalDateTime windowEnd) {
    }

    /**
     * OfflineCycleResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record OfflineCycleResult(
            Long runId,
            Long tenantId,
            String status,
            long loadedRows,
            long failedRows,
            String errorMessage,
            List<OfflineCycleStepResult> steps) {
    }

    /**
     * OfflineCycleStepResult 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * AggregationWindow 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record AggregationWindow(LocalDateTime from, LocalDateTime to, boolean due) {
    }
}
