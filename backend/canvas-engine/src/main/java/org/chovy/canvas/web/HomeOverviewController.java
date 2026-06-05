package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.infrastructure.doris.DailyStatsDTO;
import org.chovy.canvas.infrastructure.doris.DorisQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 首页概览 HTTP 控制器，根路由为 {@code /canvas/home}。
 */
@RestController
@RequestMapping("/canvas/home")
public class HomeOverviewController {

    /** 画布 Mapper，用于统计已发布画布。 */
    private final CanvasMapper canvasMapper;
    /** 执行记录 Mapper，用于统计首页执行数据。 */
    private final CanvasExecutionMapper executionMapper;
    /** Doris 查询服务；启用后优先承载首页 OLAP 聚合查询。 */
    private final DorisQueryService dorisQueryService;

    public HomeOverviewController(CanvasMapper canvasMapper,
                                  CanvasExecutionMapper executionMapper) {
        this(canvasMapper, executionMapper, null);
    }

    @Autowired
    public HomeOverviewController(CanvasMapper canvasMapper,
                                  CanvasExecutionMapper executionMapper,
                                  DorisQueryService dorisQueryService) {
        this.canvasMapper = canvasMapper;
        this.executionMapper = executionMapper;
        this.dorisQueryService = dorisQueryService;
    }

    @GetMapping("/overview")
    public Mono<R<HomeOverviewDTO>> overview(@RequestParam(name = "days", defaultValue = "7") int days) {
        int normalizedDays = days <= 0 || days > 30 ? 7 : days;
        return Mono.fromCallable(() -> R.ok(buildOverview(normalizedDays)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private HomeOverviewDTO buildOverview(int days) {
        LocalDate today = LocalDate.now();
        LocalDate since = today.minusDays(days - 1L);

        List<CanvasDO> publishedCanvases = safeList(canvasMapper.selectList(
                        new LambdaQueryWrapper<CanvasDO>()
                                .eq(CanvasDO::getStatus, CanvasStatusEnum.PUBLISHED.getCode())
                ))
                .stream()
                .filter(canvas -> CanvasStatusEnum.PUBLISHED.getCode().equals(canvas.getStatus()))
                .toList();
        Map<Long, CanvasDO> canvasById = publishedCanvases.stream()
                .filter(canvas -> canvas.getId() != null)
                .collect(Collectors.toMap(CanvasDO::getId, canvas -> canvas, (left, right) -> left, LinkedHashMap::new));

        return buildOverviewFromDoris(days, since, today, publishedCanvases, canvasById)
                .orElseGet(() -> buildOverviewFromMysql(days, since, today, publishedCanvases, canvasById));
    }

    private Optional<HomeOverviewDTO> buildOverviewFromDoris(
            int days,
            LocalDate since,
            LocalDate today,
            List<CanvasDO> publishedCanvases,
            Map<Long, CanvasDO> canvasById
    ) {
        if (dorisQueryService == null || !dorisQueryService.available()) {
            return Optional.empty();
        }
        List<DailyStatsDTO> rows = safeList(dorisQueryService.getDailyStats(since, today));
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        List<DailyStatsDTO> relevantRows = rows.stream()
                .filter(row -> row.canvasId() != null && canvasById.containsKey(row.canvasId()))
                .toList();

        long totalExecutions = relevantRows.stream().mapToLong(row -> value(row.totalExecutions())).sum();
        if (relevantRows.isEmpty() || totalExecutions <= 0) {
            return Optional.empty();
        }
        long failedExecutions = relevantRows.stream().mapToLong(row -> value(row.failCount())).sum();
        long successExecutions = relevantRows.stream().mapToLong(row -> value(row.successCount())).sum();
        long uniqueUsers = relevantRows.stream().mapToLong(row -> value(row.uniqueUsers())).sum();

        return Optional.of(new HomeOverviewDTO(
                new RangeDTO(days, since.toString(), today.toString()),
                new SummaryDTO(
                        publishedCanvases.size(),
                        totalExecutions,
                        uniqueUsers,
                        failedExecutions,
                        formatRate(successExecutions, totalExecutions)
                ),
                buildTrendFromDoris(relevantRows, since, today),
                buildTopCanvasesFromDoris(relevantRows, canvasById),
                buildAttentionItemsFromDoris(totalExecutions, failedExecutions, relevantRows, canvasById)
        ));
    }

    private HomeOverviewDTO buildOverviewFromMysql(
            int days,
            LocalDate since,
            LocalDate today,
            List<CanvasDO> publishedCanvases,
            Map<Long, CanvasDO> canvasById
    ) {
        LocalDateTime sinceTime = since.atStartOfDay();
        LocalDateTime untilTime = today.plusDays(1).atStartOfDay();

        List<CanvasExecutionDO> executions = canvasById.isEmpty()
                ? List.of()
                : safeList(executionMapper.selectList(
                        new LambdaQueryWrapper<CanvasExecutionDO>()
                                .ge(CanvasExecutionDO::getCreatedAt, sinceTime)
                                .lt(CanvasExecutionDO::getCreatedAt, untilTime)
                ));
        List<CanvasExecutionDO> relevantExecutions = executions.stream()
                .filter(execution -> execution.getCanvasId() != null && canvasById.containsKey(execution.getCanvasId()))
                .toList();

        long totalExecutions = relevantExecutions.size();
        long failedExecutions = relevantExecutions.stream()
                .filter(execution -> hasStatus(execution, ExecutionStatus.FAILED))
                .count();
        long successExecutions = relevantExecutions.stream()
                .filter(execution -> hasStatus(execution, ExecutionStatus.SUCCESS))
                .count();
        long uniqueUsers = relevantExecutions.stream()
                .map(CanvasExecutionDO::getUserId)
                .filter(userId -> userId != null && !userId.isBlank())
                .distinct()
                .count();

        return new HomeOverviewDTO(
                new RangeDTO(days, since.toString(), today.toString()),
                new SummaryDTO(
                        publishedCanvases.size(),
                        totalExecutions,
                        uniqueUsers,
                        failedExecutions,
                        formatRate(successExecutions, totalExecutions)
                ),
                buildTrend(relevantExecutions, since, today),
                buildTopCanvases(relevantExecutions, canvasById),
                buildAttentionItems(totalExecutions, failedExecutions, relevantExecutions, canvasById)
        );
    }

    private List<TrendPointDTO> buildTrendFromDoris(List<DailyStatsDTO> rows, LocalDate since, LocalDate today) {
        Map<LocalDate, long[]> byDate = new LinkedHashMap<>();
        LocalDate cursor = since;
        while (!cursor.isAfter(today)) {
            byDate.put(cursor, new long[]{0L, 0L});
            cursor = cursor.plusDays(1);
        }
        rows.stream()
                .filter(row -> row.statDate() != null)
                .forEach(row -> byDate.computeIfPresent(row.statDate(), (date, counts) -> {
                    counts[0] += value(row.totalExecutions());
                    counts[1] += value(row.failCount());
                    return counts;
                }));
        return byDate.entrySet().stream()
                .map(entry -> new TrendPointDTO(entry.getKey().toString(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    private List<TopCanvasDTO> buildTopCanvasesFromDoris(List<DailyStatsDTO> rows, Map<Long, CanvasDO> canvasById) {
        return rows.stream()
                .collect(Collectors.groupingBy(DailyStatsDTO::canvasId, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    Long canvasId = entry.getKey();
                    List<DailyStatsDTO> canvasRows = entry.getValue();
                    long total = canvasRows.stream().mapToLong(row -> value(row.totalExecutions())).sum();
                    long failed = canvasRows.stream().mapToLong(row -> value(row.failCount())).sum();
                    long success = canvasRows.stream().mapToLong(row -> value(row.successCount())).sum();
                    long uniqueUsers = canvasRows.stream().mapToLong(row -> value(row.uniqueUsers())).sum();
                    CanvasDO canvas = canvasById.get(canvasId);
                    return new TopCanvasDTO(
                            canvasId,
                            canvasName(canvasId, canvas, canvasRows),
                            total,
                            uniqueUsers,
                            formatRate(success, total),
                            failed
                    );
                })
                .sorted(Comparator.comparingLong(TopCanvasDTO::total).reversed()
                        .thenComparing(TopCanvasDTO::canvasId))
                .limit(5)
                .toList();
    }

    private List<AttentionItemDTO> buildAttentionItemsFromDoris(
            long totalExecutions,
            long failedExecutions,
            List<DailyStatsDTO> rows,
            Map<Long, CanvasDO> canvasById
    ) {
        List<AttentionItemDTO> items = new ArrayList<>();
        if (totalExecutions == 0) {
            items.add(noRecentExecutionsItem());
        }
        if (failedExecutions > 0) {
            rows.stream()
                    .collect(Collectors.groupingBy(DailyStatsDTO::canvasId,
                            Collectors.summingLong(row -> value(row.failCount()))))
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() > 0)
                    .max(Map.Entry.comparingByValue())
                    .ifPresent(entry -> {
                        Long canvasId = entry.getKey();
                        CanvasDO canvas = canvasById.get(canvasId);
                        items.add(new AttentionItemDTO(
                                canvasId,
                                canvas != null ? canvas.getName() : String.valueOf(canvasId),
                                "HAS_FAILURES",
                                "存在 " + entry.getValue() + " 次失败执行需要关注",
                                "warning"
                        ));
                    });
        }
        return items;
    }

    private List<TrendPointDTO> buildTrend(List<CanvasExecutionDO> executions, LocalDate since, LocalDate today) {
        Map<LocalDate, List<CanvasExecutionDO>> byDate = executions.stream()
                .filter(execution -> execution.getCreatedAt() != null)
                .collect(Collectors.groupingBy(execution -> execution.getCreatedAt().toLocalDate()));

        List<TrendPointDTO> trend = new ArrayList<>();
        LocalDate cursor = since;
        while (!cursor.isAfter(today)) {
            List<CanvasExecutionDO> dayExecutions = byDate.getOrDefault(cursor, List.of());
            long failed = dayExecutions.stream()
                    .filter(execution -> hasStatus(execution, ExecutionStatus.FAILED))
                    .count();
            trend.add(new TrendPointDTO(cursor.toString(), dayExecutions.size(), failed));
            cursor = cursor.plusDays(1);
        }
        return trend;
    }

    private List<TopCanvasDTO> buildTopCanvases(List<CanvasExecutionDO> executions, Map<Long, CanvasDO> canvasById) {
        return executions.stream()
                .collect(Collectors.groupingBy(CanvasExecutionDO::getCanvasId))
                .entrySet()
                .stream()
                .map(entry -> {
                    Long canvasId = entry.getKey();
                    List<CanvasExecutionDO> canvasExecutions = entry.getValue();
                    long failed = canvasExecutions.stream()
                            .filter(execution -> hasStatus(execution, ExecutionStatus.FAILED))
                            .count();
                    long success = canvasExecutions.stream()
                            .filter(execution -> hasStatus(execution, ExecutionStatus.SUCCESS))
                            .count();
                    long uniqueUsers = canvasExecutions.stream()
                            .map(CanvasExecutionDO::getUserId)
                            .filter(userId -> userId != null && !userId.isBlank())
                            .distinct()
                            .count();
                    CanvasDO canvas = canvasById.get(canvasId);
                    return new TopCanvasDTO(
                            canvasId,
                            canvas != null ? canvas.getName() : String.valueOf(canvasId),
                            canvasExecutions.size(),
                            uniqueUsers,
                            formatRate(success, canvasExecutions.size()),
                            failed
                    );
                })
                .sorted(Comparator.comparingLong(TopCanvasDTO::total).reversed()
                        .thenComparing(TopCanvasDTO::canvasId))
                .limit(5)
                .toList();
    }

    private List<AttentionItemDTO> buildAttentionItems(
            long totalExecutions,
            long failedExecutions,
            List<CanvasExecutionDO> executions,
            Map<Long, CanvasDO> canvasById
    ) {
        List<AttentionItemDTO> items = new ArrayList<>();
        if (totalExecutions == 0) {
            items.add(noRecentExecutionsItem());
        }
        if (failedExecutions > 0) {
            executions.stream()
                    .filter(execution -> hasStatus(execution, ExecutionStatus.FAILED))
                    .collect(Collectors.groupingBy(CanvasExecutionDO::getCanvasId, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .ifPresent(entry -> {
                        Long canvasId = entry.getKey();
                        CanvasDO canvas = canvasById.get(canvasId);
                        items.add(new AttentionItemDTO(
                                canvasId,
                                canvas != null ? canvas.getName() : String.valueOf(canvasId),
                                "HAS_FAILURES",
                                "存在 " + entry.getValue() + " 次失败执行需要关注",
                                "warning"
                        ));
                    });
        }
        return items;
    }

    private AttentionItemDTO noRecentExecutionsItem() {
        return new AttentionItemDTO(
                0L,
                "全部旅程",
                "NO_RECENT_EXECUTIONS",
                "最近暂无执行记录",
                "info"
        );
    }

    private String canvasName(Long canvasId, CanvasDO canvas, List<DailyStatsDTO> rows) {
        if (canvas != null && canvas.getName() != null && !canvas.getName().isBlank()) {
            return canvas.getName();
        }
        return rows.stream()
                .map(DailyStatsDTO::canvasName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(String.valueOf(canvasId));
    }

    private boolean hasStatus(CanvasExecutionDO execution, ExecutionStatus status) {
        return Objects.equals(execution.getStatus(), status.getCode());
    }

    private String formatRate(long successExecutions, long totalExecutions) {
        if (totalExecutions <= 0) {
            return "0%";
        }
        double rate = successExecutions * 100.0 / totalExecutions;
        return String.format("%.1f%%", rate);
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    public record HomeOverviewDTO(
            /** 统计时间范围。 */
            RangeDTO range,
            /** 汇总指标。 */
            SummaryDTO summary,
            /** 每日执行趋势。 */
            List<TrendPointDTO> trend,
            /** 执行量靠前的画布。 */
            List<TopCanvasDTO> topCanvases,
            /** 首页关注项。 */
            List<AttentionItemDTO> attentionItems
    ) {
    }

    public record RangeDTO(
            /** 统计天数。 */
            int days,
            /** 起始日期（含）。 */
            String since,
            /** 截止日期（含）。 */
            String until
    ) {
    }

    public record SummaryDTO(
            /** 已发布画布数量。 */
            long publishedCanvasCount,
            /** 执行总次数。 */
            long totalExecutions,
            /** 去重用户数。 */
            long uniqueUsers,
            /** 失败执行次数。 */
            long failedExecutions,
            /** 成功率展示值。 */
            String successRate
    ) {
    }

    public record TrendPointDTO(
            /** 日期。 */
            String date,
            /** 当日执行次数。 */
            long total,
            /** 当日失败次数。 */
            long failed
    ) {
    }

    public record TopCanvasDTO(
            /** 画布 ID。 */
            Long canvasId,
            /** 画布名称。 */
            String name,
            /** 执行总次数。 */
            long total,
            /** 去重用户数。 */
            long uniqueUsers,
            /** 成功率展示值。 */
            String successRate,
            /** 失败执行次数。 */
            long failed
    ) {
    }

    public record AttentionItemDTO(
            /** 关联画布 ID。 */
            Long canvasId,
            /** 关联名称。 */
            String name,
            /** 提醒类型。 */
            String type,
            /** 提醒文案。 */
            String message,
            /** 严重级别。 */
            String severity
    ) {
    }
}
