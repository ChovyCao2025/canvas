package org.chovy.canvas.web;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.R;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.chovy.canvas.dal.dataobject.CanvasConversionAttributionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionStatsDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.CanvasConversionAttributionMapper;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.chovy.canvas.infrastructure.doris.DailyStatsDTO;
import org.chovy.canvas.infrastructure.doris.DorisQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 活动效果分析 API（设计文档第二十一章 21.3节）。
 * GET /canvas/{id}/stats  整体执行统计（时间范围）
 * GET /canvas/{id}/funnel 节点漏斗转化
 * GET /canvas/{id}/trend  每日执行量趋势
 */
@RestController
@RequestMapping("/canvas/{id}")
public class CanvasStatsController {

    /** 执行记录 Mapper，用于查询画布执行统计。 */
    private final CanvasExecutionMapper executionMapper;
    /** 执行轨迹 Mapper，用于查询节点轨迹和漏斗。 */
    private final CanvasExecutionTraceMapper traceMapper;
    /** 日聚合统计 Mapper，用于避免统计接口扫描执行明细。 */
    private final CanvasExecutionStatsMapper statsMapper;
    /** Doris 查询服务；启用后优先承载 OLAP 明细和聚合查询。 */
    private final DorisQueryService dorisQueryService;
    private final MessageSendRecordMapper messageSendRecordMapper;
    private final CanvasConversionAttributionMapper attributionMapper;

    /**
     * 创建 CanvasStatsController 实例并注入 web 场景依赖。
     * @param executionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param traceMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CanvasStatsController(CanvasExecutionMapper executionMapper,
                                 CanvasExecutionTraceMapper traceMapper) {
        this(executionMapper, traceMapper, null, null, null, null);
    }

    /**
     * 创建 CanvasStatsController 实例并注入 web 场景依赖。
     * @param executionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param traceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param statsMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisQueryService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CanvasStatsController(CanvasExecutionMapper executionMapper,
                                 CanvasExecutionTraceMapper traceMapper,
                                 CanvasExecutionStatsMapper statsMapper,
                                 DorisQueryService dorisQueryService) {
        this(executionMapper, traceMapper, statsMapper, dorisQueryService, null, null);
    }

    /**
     * 创建 CanvasStatsController 实例并注入 web 场景依赖。
     * @param executionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param traceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param statsMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisQueryService 依赖组件，用于完成数据访问或外部能力调用。
     * @param messageSendRecordMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param attributionMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public CanvasStatsController(CanvasExecutionMapper executionMapper,
                                 CanvasExecutionTraceMapper traceMapper,
                                 CanvasExecutionStatsMapper statsMapper,
                                 DorisQueryService dorisQueryService,
                                 MessageSendRecordMapper messageSendRecordMapper,
                                 CanvasConversionAttributionMapper attributionMapper) {
        this.executionMapper = executionMapper;
        this.traceMapper = traceMapper;
        this.statsMapper = statsMapper;
        this.dorisQueryService = dorisQueryService;
        this.messageSendRecordMapper = messageSendRecordMapper;
        this.attributionMapper = attributionMapper;
    }

    /**
     * 查询指定执行实例的节点轨迹。
     *
     * <p>接口先确认执行实例属于当前画布，避免通过执行 ID 越权读取其他画布轨迹。
     * 轨迹优先来自 Doris 明细表，Doris 无数据时回退到 MySQL 轨迹表，并统一输出前端画布可视化需要的字段。</p>
     *
     * @param id 画布 ID，用于校验执行实例归属。
     * @param executionId 执行实例 ID。
     * @return 异步返回节点轨迹列表，未匹配到归属时返回空列表。
     */
    @GetMapping("/execution/{executionId}/trace")
    public Mono<R<List<Map<String, Object>>>> getTrace(@PathVariable Long id,
                                                       @PathVariable String executionId) {
        return Mono.fromCallable(() -> {
            CanvasExecutionDO execution = executionMapper.selectById(executionId);
            if (execution == null || !Objects.equals(execution.getCanvasId(), id)) {
                return List.<Map<String, Object>>of();
            }
            List<DorisQueryService.TraceRowDTO> dorisRows = dorisQueryService == null
                    ? List.of()
                    : dorisQueryService.getExecutionTrace(executionId);
            if (!dorisRows.isEmpty()) {
                // Doris 是生产 OLAP 主路径，字段名在这里统一转换成前端约定的 Map key。
                return toTraceMapsFromDoris(dorisRows);
            }
            List<CanvasExecutionTraceDO> all =
                    traceMapper.selectList(
                            new LambdaQueryWrapper<
                                    CanvasExecutionTraceDO>()
                                    .eq(CanvasExecutionTraceDO::getExecutionId,
                                            executionId)
                                    .orderByAsc(CanvasExecutionTraceDO::getStartedAt));

            return toTraceMapsFromMysql(all);
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 查询画布最近执行记录，供前端执行轨迹选择器使用。
     *
     * <p>接口按创建时间倒序读取最近记录，并把返回数量限制在 100 条以内，避免轨迹选择器误触发大范围明细扫描。
     * 返回字段保持轻量，只包含执行 ID、触发类型、状态、用户和创建时间。</p>
     *
     * @param id 画布 ID。
     * @param size 期望返回数量，服务端会限制最大值。
     * @return 异步返回最近执行记录列表。
     */
    @GetMapping("/executions")
    public Mono<R<List<Map<String, Object>>>> recentExecutions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> {
            List<org.chovy.canvas.dal.dataobject.CanvasExecutionDO> execs =
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                    executionMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                    org.chovy.canvas.dal.dataobject.CanvasExecutionDO>()
                                    .eq(org.chovy.canvas.dal.dataobject.CanvasExecutionDO::getCanvasId, id)
                                    .orderByDesc(org.chovy.canvas.dal.dataobject.CanvasExecutionDO::getCreatedAt)
                                    .last("LIMIT " + Math.min(size, 100)));
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            return execs.stream().map(e -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put(MapFieldKeys.ID, e.getId());
                m.put(MapFieldKeys.TRIGGER_TYPE, e.getTriggerType());
                m.put(MapFieldKeys.STATUS, e.getStatus());
                m.put(MapFieldKeys.USER_ID, e.getUserId());
                m.put(MapFieldKeys.CREATED_AT, e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
                // 汇总前面计算出的状态和明细，返回给调用方。
                return m;
            }).collect(java.util.stream.Collectors.toList());
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).map(R::ok);
    }
    /**
     * 查询画布在指定时间范围内的执行概览统计。
     *
     * <p>统计口径包含总执行数、成功数、失败数、暂停/运行中数量、成功率和去重用户数。
     * 数据源按 Doris 日聚合、MySQL 日聚合表、执行明细扫描的顺序回退，保证生产查询优先走 OLAP，
     * 本地或聚合缺失时仍能给出可用结果。</p>
     *
     * @param id 画布 ID。
     * @param days 未传 {@code since}/{@code until} 时使用的最近天数。
     * @param since 起始日期（含），格式为 {@code yyyy-MM-dd}。
     * @param until 截止日期（含），格式为 {@code yyyy-MM-dd}。
     * @return 异步返回画布执行概览指标。
     */
    @GetMapping("/stats")
    public Mono<R<Map<String, Object>>> stats(
            @PathVariable Long id,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {
        return Mono.fromCallable(() -> {
            OverviewRange range = overviewRange(days, since, until);
            // 按生产 OLAP、预聚合、明细扫描逐级回退，避免某一层缺数据导致统计接口不可用。
            return statsFromDoris(id, range)
                    .or(() -> statsFromAggregateTable(id, range))
                    .orElseGet(() -> statsFromExecutionScan(id, range));
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 查询画布节点漏斗统计。
     *
     * <p>接口按节点聚合进入、成功、失败、跳过次数和平均耗时，前端用这些数据在画布节点上叠加漏斗视图。
     * Doris 节点聚合可用时优先返回；否则回退到 MySQL mapper 的漏斗查询并补齐耗时秒数字段。</p>
     *
     * @param id 画布 ID。
     * @return 异步返回节点粒度的漏斗统计列表。
     */
    @GetMapping("/funnel")
    public Mono<R<List<Map<String, Object>>>> funnel(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            List<Map<String, Object>> dorisRows = funnelFromDoris(id);
            if (!dorisRows.isEmpty()) {
                return dorisRows;
            }
            return normalizeFunnelRows(traceMapper.selectFunnelByCanvasId(id));
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 查询画布每日执行趋势。
     *
     * <p>趋势会补齐时间范围内没有执行的日期，保证前端折线图横轴连续。数据源回退顺序与概览统计一致：
     * Doris 日聚合优先，其次 MySQL 聚合表，最后按执行明细按天聚合。</p>
     *
     * @param id 画布 ID。
     * @param days 未传日期范围时使用的最近天数。
     * @param since 起始日期（含）。
     * @param until 截止日期（含）。
     * @return 异步返回按日期排序的执行趋势点。
     */
    @GetMapping("/trend")
    public Mono<R<List<Map<String, Object>>>> trend(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {
        return Mono.fromCallable(() -> {
            OverviewRange range = overviewRange(days, since, until);
            return trendFromDoris(id, range)
                    .or(() -> trendFromAggregateTable(id, range))
                    .orElseGet(() -> trendFromExecutionScan(id, range));
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }
    /**
     * 查询画布消息回执状态分布。
     *
     * <p>接口按发送记录状态聚合计数，用于统计送达、失败、退订等下游触达结果。
     * 若当前运行环境未装配消息发送 mapper，则返回空 Map，避免影响画布基础统计页。</p>
     *
     * @param id 画布 ID。
     * @return 异步返回按回执状态分组的计数 Map。
     */
    @GetMapping("/receipts")
    public Mono<R<Map<String, Object>>> receipts(@PathVariable Long id) {
        return Mono.fromCallable(() -> receiptCounts(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }
    /**
     * 查询画布转化归因汇总。
     *
     * <p>接口聚合归因转化次数、加权转化金额、被归因触达数和使用过的归因模型。
     * 归因表未装配或暂无数据时返回 LAST_TOUCH 默认模型和零值指标，便于前端稳定展示。</p>
     *
     * @param id 画布 ID。
     * @return 异步返回画布级转化归因汇总指标。
     */
    @GetMapping("/attribution-summary")
    public Mono<R<Map<String, Object>>> attributionSummary(@PathVariable Long id) {
        return Mono.fromCallable(() -> attributionTotals(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 执行 receiptCounts 流程，围绕 receipt counts 完成校验、计算或结果组装。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回 receiptCounts 流程生成的业务结果。
     */
    private Map<String, Object> receiptCounts(Long canvasId) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (messageSendRecordMapper == null) {
            return result;
        }
        List<Map<String, Object>> rows = messageSendRecordMapper.selectMaps(
                new QueryWrapper<MessageSendRecordDO>()
                        .select("status", "COUNT(*) AS count")
                        .eq("canvas_id", canvasId)
                        .groupBy("status"));
        for (Map<String, Object> row : rows == null ? List.<Map<String, Object>>of() : rows) {
            Object status = row.get("status");
            if (status == null) {
                // MyBatis 在不同数据库驱动下可能返回大写列名，这里兼容回执状态字段。
                status = row.get("STATUS");
            }
            if (status == null) {
                continue;
            }
            result.put(status.toString().toLowerCase(Locale.ROOT), toLong(rowValue(row, "count")));
        }
        return result;
    }

    /**
     * 执行 attributionTotals 流程，围绕 attribution totals 完成校验、计算或结果组装。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回 attributionTotals 流程生成的业务结果。
     */
    private Map<String, Object> attributionTotals(Long canvasId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversions", 0L);
        result.put("conversionAmount", BigDecimal.ZERO);
        result.put("attributedSends", 0L);
        result.put("model", "LAST_TOUCH");
        result.put("models", "LAST_TOUCH");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (attributionMapper == null) {
            return result;
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<Map<String, Object>> rows = attributionMapper.selectMaps(
                new QueryWrapper<CanvasConversionAttributionDO>()
                        .select("COUNT(DISTINCT event_log_id) AS conversions",
                                "COALESCE(SUM(COALESCE(conversion_amount, 0) * COALESCE(attribution_weight, 1)), 0) AS weightedConversionAmount",
                                "COUNT(DISTINCT NULLIF(send_record_id, 0)) AS attributedSends",
                                "GROUP_CONCAT(DISTINCT attribution_model ORDER BY attribution_model) AS models")
                        .eq("canvas_id", canvasId));
        if (rows == null || rows.isEmpty()) {
            return result;
        }
        Map<String, Object> row = rows.getFirst();
        result.put("conversions", toLong(rowValue(row, "conversions")));
        Object amount = rowValue(row, "weightedConversionAmount");
        if (amount == null) {
            amount = rowValue(row, "conversionAmount");
        }
        result.put("conversionAmount", toBigDecimal(amount));
        result.put("attributedSends", toLong(rowValue(row, "attributedSends")));
        Object models = rowValue(row, "models");
        String modelList = models == null || models.toString().isBlank() ? "LAST_TOUCH" : models.toString();
        result.put("model", modelList);
        result.put("models", modelList);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param days days 参数，用于 overviewRange 流程中的校验、计算或对象转换。
     * @param since since 参数，用于 overviewRange 流程中的校验、计算或对象转换。
     * @param until until 参数，用于 overviewRange 流程中的校验、计算或对象转换。
     * @return 返回 overviewRange 流程生成的业务结果。
     */
    private OverviewRange overviewRange(int days, String since, String until) {
        LocalDate today = LocalDate.now();
        LocalDate untilDate = until != null && !until.isBlank() ? LocalDate.parse(until) : today;
        LocalDate sinceDate = since != null && !since.isBlank()
                ? LocalDate.parse(since)
                : untilDate.minusDays(Math.max(1, days) - 1L);
        if (sinceDate.isAfter(untilDate)) {
            throw new IllegalArgumentException("since must be on or before until");
        }
        return new OverviewRange(sinceDate, untilDate);
    }

    /**
     * 执行 statsFromDoris 流程，围绕 stats from doris 完成校验、计算或结果组装。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param range range 参数，用于 statsFromDoris 流程中的校验、计算或对象转换。
     * @return 返回 statsFromDoris 流程生成的业务结果。
     */
    private Optional<Map<String, Object>> statsFromDoris(Long canvasId, OverviewRange range) {
        if (dorisQueryService == null) {
            return Optional.empty();
        }
        List<DailyStatsDTO> rows = dorisQueryService.getDailyStats(canvasId, range.sinceDate(), range.untilDate());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        // Doris 返回的是按天预聚合指标，接口需要折叠成画布范围内的总览指标。
        long total = rows.stream().mapToLong(row -> nullToZero(row.totalExecutions())).sum();
        long success = rows.stream().mapToLong(row -> nullToZero(row.successCount())).sum();
        long failed = rows.stream().mapToLong(row -> nullToZero(row.failCount())).sum();
        long paused = rows.stream().mapToLong(row -> nullToZero(row.runningCount())).sum();
        long uniqueUsers = rows.stream().mapToLong(row -> nullToZero(row.uniqueUsers())).sum();
        return Optional.of(statsMap(total, success, failed, paused, uniqueUsers));
    }

    /**
     * 执行 statsFromAggregateTable 流程，围绕 stats from aggregate table 完成校验、计算或结果组装。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param range range 参数，用于 statsFromAggregateTable 流程中的校验、计算或对象转换。
     * @return 返回 statsFromAggregateTable 流程生成的业务结果。
     */
    private Optional<Map<String, Object>> statsFromAggregateTable(Long canvasId, OverviewRange range) {
        List<CanvasExecutionStatsDO> rows = aggregateRows(canvasId, range);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        long total = rows.stream().mapToLong(row -> nullToZero(row.getTotalCount())).sum();
        long success = rows.stream().mapToLong(row -> nullToZero(row.getSuccessCount())).sum();
        long failed = rows.stream().mapToLong(row -> nullToZero(row.getFailCount())).sum();
        long paused = rows.stream().mapToLong(row -> nullToZero(row.getPausedCount())).sum();
        long uniqueUsers = rows.stream().mapToLong(row -> nullToZero(row.getUniqueUsers())).sum();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return Optional.of(statsMap(total, success, failed, paused, uniqueUsers));
    }

    /**
     * 执行 statsFromExecutionScan 流程，围绕 stats from execution scan 完成校验、计算或结果组装。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param range range 参数，用于 statsFromExecutionScan 流程中的校验、计算或对象转换。
     * @return 返回 statsFromExecutionScan 流程生成的业务结果。
     */
    private Map<String, Object> statsFromExecutionScan(Long canvasId, OverviewRange range) {
        // 准备本次处理所需的上下文和中间变量。
        List<CanvasExecutionDO> executions = executionsInRange(canvasId, range, false);
        long total = executions.size();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        long success = executions.stream().filter(e -> Objects.equals(e.getStatus(), 2)).count();
        long failed = executions.stream().filter(e -> Objects.equals(e.getStatus(), 3)).count();
        long paused = executions.stream().filter(e -> Objects.equals(e.getStatus(), 1)).count();
        long uniqueUsers = executions.stream()
                .map(CanvasExecutionDO::getUserId)
                .filter(userId -> userId != null && !userId.isBlank())
                .distinct()
                .count();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return statsMap(total, success, failed, paused, uniqueUsers);
    }

    /**
     * 执行 statsMap 流程，围绕 stats map 完成校验、计算或结果组装。
     *
     * @param total total 参数，用于 statsMap 流程中的校验、计算或对象转换。
     * @param success success 参数，用于 statsMap 流程中的校验、计算或对象转换。
     * @param failed failed 参数，用于 statsMap 流程中的校验、计算或对象转换。
     * @param paused paused 参数，用于 statsMap 流程中的校验、计算或对象转换。
     * @param uniqueUsers unique users 参数，用于 statsMap 流程中的校验、计算或对象转换。
     * @return 返回 statsMap 流程生成的业务结果。
     */
    private Map<String, Object> statsMap(long total, long success, long failed, long paused, long uniqueUsers) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(MapFieldKeys.TOTAL, total);
        result.put(MapFieldKeys.SUCCESS, success);
        result.put(MapFieldKeys.FAILED, failed);
        result.put(MapFieldKeys.PAUSED, paused);
        result.put(MapFieldKeys.SUCCESS_RATE, total > 0 ? String.format("%.1f%%", success * 100.0 / total) : "0%");
        result.put(MapFieldKeys.UNIQUE_USERS, uniqueUsers);
        return result;
    }

    /**
     * 执行 trendFromDoris 流程，围绕 trend from doris 完成校验、计算或结果组装。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param range range 参数，用于 trendFromDoris 流程中的校验、计算或对象转换。
     * @return 返回 trendFromDoris 流程生成的业务结果。
     */
    private Optional<List<Map<String, Object>>> trendFromDoris(Long canvasId, OverviewRange range) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (dorisQueryService == null) {
            return Optional.empty();
        }
        List<DailyStatsDTO> rows = dorisQueryService.getDailyStats(canvasId, range.sinceDate(), range.untilDate());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Map<LocalDate, Long> byDate = zeroTrend(range);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        rows.forEach(row -> byDate.merge(row.statDate(), nullToZero(row.totalExecutions()), Long::sum));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return Optional.of(toTrendPoints(byDate));
    }

    /**
     * 执行 trendFromAggregateTable 流程，围绕 trend from aggregate table 完成校验、计算或结果组装。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param range range 参数，用于 trendFromAggregateTable 流程中的校验、计算或对象转换。
     * @return 返回 trendFromAggregateTable 流程生成的业务结果。
     */
    private Optional<List<Map<String, Object>>> trendFromAggregateTable(Long canvasId, OverviewRange range) {
        List<CanvasExecutionStatsDO> rows = aggregateRows(canvasId, range);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Map<LocalDate, Long> byDate = zeroTrend(range);
        rows.forEach(row -> byDate.merge(row.getStatDate(), (long) nullToZero(row.getTotalCount()), Long::sum));
        return Optional.of(toTrendPoints(byDate));
    }

    /**
     * 执行 trendFromExecutionScan 流程，围绕 trend from execution scan 完成校验、计算或结果组装。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param range range 参数，用于 trendFromExecutionScan 流程中的校验、计算或对象转换。
     * @return 返回 trendFromExecutionScan 流程生成的业务结果。
     */
    private List<Map<String, Object>> trendFromExecutionScan(Long canvasId, OverviewRange range) {
        List<CanvasExecutionDO> executions = executionsInRange(canvasId, range, true);
        Map<LocalDate, Long> byDate = zeroTrend(range);
        for (CanvasExecutionDO e : executions) {
            if (e.getCreatedAt() == null) {
                continue;
            }
            byDate.merge(e.getCreatedAt().toLocalDate(), 1L, Long::sum);
        }
        return toTrendPoints(byDate);
    }

    /**
     * 执行 zeroTrend 流程，围绕 zero trend 完成校验、计算或结果组装。
     *
     * @param range range 参数，用于 zeroTrend 流程中的校验、计算或对象转换。
     * @return 返回 zero trend 计算得到的数量、金额或指标值。
     */
    private Map<LocalDate, Long> zeroTrend(OverviewRange range) {
        Map<LocalDate, Long> byDate = new LinkedHashMap<>();
        LocalDate cursor = range.sinceDate();
        while (!cursor.isAfter(range.untilDate())) {
            byDate.put(cursor, 0L);
            cursor = cursor.plusDays(1);
        }
        return byDate;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param LocalDate 时间参数，用于计算窗口、过期或审计时间。
     * @param byDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回组装或转换后的结果对象。
     */
    private List<Map<String, Object>> toTrendPoints(Map<LocalDate, Long> byDate) {
        List<Map<String, Object>> trend = new ArrayList<>();
        byDate.forEach((date, count) -> {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put(MapFieldKeys.DATE, date.toString());
            point.put(MapFieldKeys.COUNT, count);
            trend.add(point);
        });
        return trend;
    }

    /**
     * 执行 funnelFromDoris 流程，围绕 funnel from doris 完成校验、计算或结果组装。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回 funnelFromDoris 流程生成的业务结果。
     */
    private List<Map<String, Object>> funnelFromDoris(Long canvasId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (dorisQueryService == null) {
            return List.of();
        }
        List<DorisQueryService.NodeStatsDTO> rows = dorisQueryService.getNodeStats(
                canvasId,
                LocalDate.of(1970, 1, 1),
                LocalDate.now());
        if (rows.isEmpty()) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put(MapFieldKeys.NODE_ID, row.nodeId());
            m.put(MapFieldKeys.NODE_TYPE, row.nodeType());
            m.put(MapFieldKeys.NODE_NAME, row.nodeName());
            m.put("totalEntered", nullToZero(row.totalEntered()));
            m.put("totalSuccess", nullToZero(row.totalSuccess()));
            m.put("totalFailed", nullToZero(row.totalFailed()));
            m.put("totalSkipped", nullToZero(row.totalSkipped()));
            m.put("avgDurationMs", nullToZero(row.avgDurationMs()));
            m.put("avgDurationSec", nullToZero(row.avgDurationMs()) / 1000.0);
            // 汇总前面计算出的状态和明细，返回给调用方。
            return m;
        }).toList();
    }

    /**
     * 规范化输入值。
     *
     * @param String string 参数，用于 normalizeFunnelRows 流程中的校验、计算或对象转换。
     * @param rows rows 参数，用于 normalizeFunnelRows 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<Map<String, Object>> normalizeFunnelRows(List<Map<String, Object>> rows) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return rows.stream().map(row -> {
            Map<String, Object> normalized = new LinkedHashMap<>(row);
            if (!normalized.containsKey("avgDurationSec") && normalized.get("avgDurationMs") instanceof Number number) {
                normalized.put("avgDurationSec", number.doubleValue() / 1000.0);
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return normalized;
        }).toList();
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param range range 参数，用于 aggregateRows 流程中的校验、计算或对象转换。
     * @return 返回 aggregate rows 汇总后的集合、分页或映射视图。
     */
    private List<CanvasExecutionStatsDO> aggregateRows(Long canvasId, OverviewRange range) {
        if (statsMapper == null) {
            return List.of();
        }
        return statsMapper.selectByCanvasIdAndDateRange(canvasId, range.sinceDate(), range.untilDate());
    }

    /**
     * 执行 executionsInRange 流程，围绕 executions in range 完成校验、计算或结果组装。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param range range 参数，用于 executionsInRange 流程中的校验、计算或对象转换。
     * @param orderByAsc order by asc 参数，用于 executionsInRange 流程中的校验、计算或对象转换。
     * @return 返回 executions in range 汇总后的集合、分页或映射视图。
     */
    private List<CanvasExecutionDO> executionsInRange(Long canvasId, OverviewRange range, boolean orderByAsc) {
        LambdaQueryWrapper<CanvasExecutionDO> query = new LambdaQueryWrapper<CanvasExecutionDO>()
                .eq(CanvasExecutionDO::getCanvasId, canvasId)
                .ge(CanvasExecutionDO::getCreatedAt, range.sinceDate().atStartOfDay())
                .lt(CanvasExecutionDO::getCreatedAt, range.untilDate().plusDays(1).atStartOfDay());
        if (orderByAsc) {
            query.orderByAsc(CanvasExecutionDO::getCreatedAt);
        }
        return executionMapper.selectList(query);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param all all 参数，用于 toTraceMapsFromMysql 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private List<Map<String, Object>> toTraceMapsFromMysql(List<CanvasExecutionTraceDO> all) {
        Map<String, CanvasExecutionTraceDO> best = new LinkedHashMap<>();
        for (CanvasExecutionTraceDO t : all) {
            best.merge(t.getNodeId(), t, (a, b) -> nullToZero(b.getStatus()) > nullToZero(a.getStatus()) ? b : a);
        }
        return best.values().stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put(MapFieldKeys.NODE_ID, t.getNodeId());
            m.put(MapFieldKeys.NODE_TYPE, t.getNodeType());
            m.put(MapFieldKeys.NODE_NAME, t.getNodeName());
            m.put(MapFieldKeys.STATUS, t.getStatus());
            m.put(MapFieldKeys.ERROR_MSG, t.getErrorMsg());
            m.put(MapFieldKeys.OUTPUT_DATA, t.getOutputData());
            if (t.getDurationMs() != null) {
                m.put(MapFieldKeys.DURATION_MS, t.getDurationMs());
            // 根据前序判断结果进入后续条件分支。
            } else if (t.getStartedAt() != null && t.getFinishedAt() != null) {
                m.put(MapFieldKeys.DURATION_MS, Duration.between(t.getStartedAt(), t.getFinishedAt()).toMillis());
            }
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param all all 参数，用于 toTraceMapsFromDoris 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private List<Map<String, Object>> toTraceMapsFromDoris(List<DorisQueryService.TraceRowDTO> all) {
        Map<String, DorisQueryService.TraceRowDTO> best = new LinkedHashMap<>();
        for (DorisQueryService.TraceRowDTO t : all) {
            best.merge(t.nodeId(), t, (a, b) -> nullToZero(b.status()) > nullToZero(a.status()) ? b : a);
        }
        return best.values().stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put(MapFieldKeys.NODE_ID, t.nodeId());
            m.put(MapFieldKeys.NODE_TYPE, t.nodeType());
            m.put(MapFieldKeys.NODE_NAME, t.nodeName());
            m.put(MapFieldKeys.STATUS, t.status());
            m.put(MapFieldKeys.ERROR_MSG, t.errorMsg());
            m.put(MapFieldKeys.OUTPUT_DATA, t.outputData());
            if (t.durationMs() != null) {
                m.put(MapFieldKeys.DURATION_MS, t.durationMs());
            // 根据前序判断结果进入后续条件分支。
            } else if (t.startedAt() != null && t.finishedAt() != null) {
                m.put(MapFieldKeys.DURATION_MS, Duration.between(t.startedAt(), t.finishedAt()).toMillis());
            }
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param String string 参数，用于 rowValue 流程中的校验、计算或对象转换。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 rowValue 流程生成的业务结果。
     */
    private Object rowValue(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        String snake = key.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        if (row.containsKey(snake)) {
            return row.get(snake);
        }
        return row.get(key.toUpperCase(Locale.ROOT));
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }
        return 0L;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BigDecimal toBigDecimal(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String text && !text.isBlank()) {
            return new BigDecimal(text.trim());
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return BigDecimal.ZERO;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to zero 计算得到的数量、金额或指标值。
     */
    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to zero 计算得到的数量、金额或指标值。
     */
    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * OverviewRange 数据记录。
     */
    private record OverviewRange(LocalDate sinceDate, LocalDate untilDate) {
    }
}
