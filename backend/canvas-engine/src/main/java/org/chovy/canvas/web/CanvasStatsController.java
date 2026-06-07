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

    public CanvasStatsController(CanvasExecutionMapper executionMapper,
                                 CanvasExecutionTraceMapper traceMapper) {
        this(executionMapper, traceMapper, null, null, null, null);
    }

    public CanvasStatsController(CanvasExecutionMapper executionMapper,
                                 CanvasExecutionTraceMapper traceMapper,
                                 CanvasExecutionStatsMapper statsMapper,
                                 DorisQueryService dorisQueryService) {
        this(executionMapper, traceMapper, statsMapper, dorisQueryService, null, null);
    }

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
     * 某次执行的所有节点轨迹（前端执行轨迹可视化，14.2节）
     *
     * @param executionId 执行实例 ID
     * @return 节点轨迹列表
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
     * 画布最近 N 次执行记录（用于前端执行轨迹选择器）
     *
     * @param id   画布 ID
     * @param size 记录数量
     * @return 执行记录列表
     */
    @GetMapping("/executions")
    public Mono<R<List<Map<String, Object>>>> recentExecutions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> {
            List<org.chovy.canvas.dal.dataobject.CanvasExecutionDO> execs =
                    executionMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                    org.chovy.canvas.dal.dataobject.CanvasExecutionDO>()
                                    .eq(org.chovy.canvas.dal.dataobject.CanvasExecutionDO::getCanvasId, id)
                                    .orderByDesc(org.chovy.canvas.dal.dataobject.CanvasExecutionDO::getCreatedAt)
                                    .last("LIMIT " + Math.min(size, 100)));
            return execs.stream().map(e -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put(MapFieldKeys.ID, e.getId());
                m.put(MapFieldKeys.TRIGGER_TYPE, e.getTriggerType());
                m.put(MapFieldKeys.STATUS, e.getStatus());
                m.put(MapFieldKeys.USER_ID, e.getUserId());
                m.put(MapFieldKeys.CREATED_AT, e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
                return m;
            }).collect(java.util.stream.Collectors.toList());
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).map(R::ok);
    }

    @GetMapping("/stats")
    public Mono<R<Map<String, Object>>> stats(
            @PathVariable Long id,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {
        return Mono.fromCallable(() -> {
            OverviewRange range = overviewRange(days, since, until);
            return statsFromDoris(id, range)
                    .or(() -> statsFromAggregateTable(id, range))
                    .orElseGet(() -> statsFromExecutionScan(id, range));
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 节点漏斗（设计文档 21.3节）：聚合每个节点的进入/成功/失败/跳过次数。
     * 前端按此数据在画布上叠加漏斗可视化。
     *
     * @param id 画布 ID
     * @return 节点统计列表
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
     * 每日执行量趋势（按天聚合）
     *
     * @param id   画布 ID
     * @param days 查询天数范围
     * @return 执行趋势列表
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

    @GetMapping("/receipts")
    public Mono<R<Map<String, Object>>> receipts(@PathVariable Long id) {
        return Mono.fromCallable(() -> receiptCounts(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @GetMapping("/attribution-summary")
    public Mono<R<Map<String, Object>>> attributionSummary(@PathVariable Long id) {
        return Mono.fromCallable(() -> attributionTotals(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

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
                status = row.get("STATUS");
            }
            if (status == null) {
                continue;
            }
            result.put(status.toString().toLowerCase(Locale.ROOT), toLong(rowValue(row, "count")));
        }
        return result;
    }

    private Map<String, Object> attributionTotals(Long canvasId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversions", 0L);
        result.put("conversionAmount", BigDecimal.ZERO);
        result.put("attributedSends", 0L);
        result.put("model", "LAST_TOUCH");
        result.put("models", "LAST_TOUCH");
        if (attributionMapper == null) {
            return result;
        }
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
        return result;
    }

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

    private Optional<Map<String, Object>> statsFromDoris(Long canvasId, OverviewRange range) {
        if (dorisQueryService == null) {
            return Optional.empty();
        }
        List<DailyStatsDTO> rows = dorisQueryService.getDailyStats(canvasId, range.sinceDate(), range.untilDate());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        long total = rows.stream().mapToLong(row -> nullToZero(row.totalExecutions())).sum();
        long success = rows.stream().mapToLong(row -> nullToZero(row.successCount())).sum();
        long failed = rows.stream().mapToLong(row -> nullToZero(row.failCount())).sum();
        long paused = rows.stream().mapToLong(row -> nullToZero(row.runningCount())).sum();
        long uniqueUsers = rows.stream().mapToLong(row -> nullToZero(row.uniqueUsers())).sum();
        return Optional.of(statsMap(total, success, failed, paused, uniqueUsers));
    }

    private Optional<Map<String, Object>> statsFromAggregateTable(Long canvasId, OverviewRange range) {
        List<CanvasExecutionStatsDO> rows = aggregateRows(canvasId, range);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        long total = rows.stream().mapToLong(row -> nullToZero(row.getTotalCount())).sum();
        long success = rows.stream().mapToLong(row -> nullToZero(row.getSuccessCount())).sum();
        long failed = rows.stream().mapToLong(row -> nullToZero(row.getFailCount())).sum();
        long paused = rows.stream().mapToLong(row -> nullToZero(row.getPausedCount())).sum();
        long uniqueUsers = rows.stream().mapToLong(row -> nullToZero(row.getUniqueUsers())).sum();
        return Optional.of(statsMap(total, success, failed, paused, uniqueUsers));
    }

    private Map<String, Object> statsFromExecutionScan(Long canvasId, OverviewRange range) {
        List<CanvasExecutionDO> executions = executionsInRange(canvasId, range, false);
        long total = executions.size();
        long success = executions.stream().filter(e -> Objects.equals(e.getStatus(), 2)).count();
        long failed = executions.stream().filter(e -> Objects.equals(e.getStatus(), 3)).count();
        long paused = executions.stream().filter(e -> Objects.equals(e.getStatus(), 1)).count();
        long uniqueUsers = executions.stream()
                .map(CanvasExecutionDO::getUserId)
                .filter(userId -> userId != null && !userId.isBlank())
                .distinct()
                .count();
        return statsMap(total, success, failed, paused, uniqueUsers);
    }

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

    private Optional<List<Map<String, Object>>> trendFromDoris(Long canvasId, OverviewRange range) {
        if (dorisQueryService == null) {
            return Optional.empty();
        }
        List<DailyStatsDTO> rows = dorisQueryService.getDailyStats(canvasId, range.sinceDate(), range.untilDate());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Map<LocalDate, Long> byDate = zeroTrend(range);
        rows.forEach(row -> byDate.merge(row.statDate(), nullToZero(row.totalExecutions()), Long::sum));
        return Optional.of(toTrendPoints(byDate));
    }

    private Optional<List<Map<String, Object>>> trendFromAggregateTable(Long canvasId, OverviewRange range) {
        List<CanvasExecutionStatsDO> rows = aggregateRows(canvasId, range);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Map<LocalDate, Long> byDate = zeroTrend(range);
        rows.forEach(row -> byDate.merge(row.getStatDate(), (long) nullToZero(row.getTotalCount()), Long::sum));
        return Optional.of(toTrendPoints(byDate));
    }

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

    private Map<LocalDate, Long> zeroTrend(OverviewRange range) {
        Map<LocalDate, Long> byDate = new LinkedHashMap<>();
        LocalDate cursor = range.sinceDate();
        while (!cursor.isAfter(range.untilDate())) {
            byDate.put(cursor, 0L);
            cursor = cursor.plusDays(1);
        }
        return byDate;
    }

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

    private List<Map<String, Object>> funnelFromDoris(Long canvasId) {
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
            return m;
        }).toList();
    }

    private List<Map<String, Object>> normalizeFunnelRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().map(row -> {
            Map<String, Object> normalized = new LinkedHashMap<>(row);
            if (!normalized.containsKey("avgDurationSec") && normalized.get("avgDurationMs") instanceof Number number) {
                normalized.put("avgDurationSec", number.doubleValue() / 1000.0);
            }
            return normalized;
        }).toList();
    }

    private List<CanvasExecutionStatsDO> aggregateRows(Long canvasId, OverviewRange range) {
        if (statsMapper == null) {
            return List.of();
        }
        return statsMapper.selectByCanvasIdAndDateRange(canvasId, range.sinceDate(), range.untilDate());
    }

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
            } else if (t.getStartedAt() != null && t.getFinishedAt() != null) {
                m.put(MapFieldKeys.DURATION_MS, Duration.between(t.getStartedAt(), t.getFinishedAt()).toMillis());
            }
            return m;
        }).collect(Collectors.toList());
    }

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
            } else if (t.startedAt() != null && t.finishedAt() != null) {
                m.put(MapFieldKeys.DURATION_MS, Duration.between(t.startedAt(), t.finishedAt()).toMillis());
            }
            return m;
        }).collect(Collectors.toList());
    }

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

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }
        return 0L;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String text && !text.isBlank()) {
            return new BigDecimal(text.trim());
        }
        return BigDecimal.ZERO;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private record OverviewRange(LocalDate sinceDate, LocalDate untilDate) {
    }
}
