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

    /**
     * 创建 HomeOverviewController 实例并注入 web 场景依赖。
     * @param canvasMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param executionMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public HomeOverviewController(CanvasMapper canvasMapper,
                                  CanvasExecutionMapper executionMapper) {
        this(canvasMapper, executionMapper, null);
    }

    /**
     * 创建 HomeOverviewController 实例并注入 web 场景依赖。
     * @param canvasMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param executionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisQueryService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public HomeOverviewController(CanvasMapper canvasMapper,
                                  CanvasExecutionMapper executionMapper,
                                  DorisQueryService dorisQueryService) {
        this.canvasMapper = canvasMapper;
        this.executionMapper = executionMapper;
        this.dorisQueryService = dorisQueryService;
    }
    /**
     * 查询首页营销旅程执行概览。
     *
     * <p>接口统计最近一段时间内已发布画布的执行总量、失败量、去重用户、执行趋势、
     * Top 画布和需要关注的异常项。{@code days} 会被限制在 1 到 30 天之间，避免首页请求造成过大明细扫描。</p>
     *
     * @param days 统计天数，非法或超过上限时回退为 7 天。
     * @return 异步返回首页概览视图。
     */
    @GetMapping("/overview")
    public Mono<R<HomeOverviewDTO>> overview(@RequestParam(name = "days", defaultValue = "7") int days) {
        int normalizedDays = days <= 0 || days > 30 ? 7 : days;
        return Mono.fromCallable(() -> R.ok(buildOverview(normalizedDays)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param days days 参数，用于 buildOverview 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private HomeOverviewDTO buildOverview(int days) {
        LocalDate today = LocalDate.now();
        LocalDate since = today.minusDays(days - 1L);

        // 首页只展示已发布画布，避免草稿和归档旅程干扰运行态指标。
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

        // Doris 可用且有有效执行数据时走 OLAP 聚合，否则回退 MySQL 明细保证本地和测试环境可用。
        return buildOverviewFromDoris(days, since, today, publishedCanvases, canvasById)
                .orElseGet(() -> buildOverviewFromMysql(days, since, today, publishedCanvases, canvasById));
    }

    /**
     * 构建业务对象或响应数据。
     *
     * @param days days 参数，用于 buildOverviewFromDoris 流程中的校验、计算或对象转换。
     * @param since since 参数，用于 buildOverviewFromDoris 流程中的校验、计算或对象转换。
     * @param today today 参数，用于 buildOverviewFromDoris 流程中的校验、计算或对象转换。
     * @param publishedCanvases published canvases 参数，用于 buildOverviewFromDoris 流程中的校验、计算或对象转换。
     * @param canvasById 业务对象 ID，用于定位具体记录。
     * @return 返回组装或转换后的结果对象。
     */
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
            // Doris 可能已连通但聚合任务尚未产出数据，此时不能用空聚合覆盖 MySQL 明细结果。
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

    /**
     * 构建业务对象或响应数据。
     *
     * @param days days 参数，用于 buildOverviewFromMysql 流程中的校验、计算或对象转换。
     * @param since since 参数，用于 buildOverviewFromMysql 流程中的校验、计算或对象转换。
     * @param today today 参数，用于 buildOverviewFromMysql 流程中的校验、计算或对象转换。
     * @param publishedCanvases published canvases 参数，用于 buildOverviewFromMysql 流程中的校验、计算或对象转换。
     * @param canvasById 业务对象 ID，用于定位具体记录。
     * @return 返回组装或转换后的结果对象。
     */
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
                /**
                 * 按安全边界裁剪或保护输入值。
                 *
                 * @return 返回 safeList 流程生成的业务结果。
                 */
                : safeList(executionMapper.selectList(
                        new LambdaQueryWrapper<CanvasExecutionDO>()
                                .ge(CanvasExecutionDO::getCreatedAt, sinceTime)
                                .lt(CanvasExecutionDO::getCreatedAt, untilTime)
                ));
        List<CanvasExecutionDO> relevantExecutions = executions.stream()
                .filter(execution -> execution.getCanvasId() != null && canvasById.containsKey(execution.getCanvasId()))
                .toList();

        // MySQL fallback 按执行明细现场聚合，统计口径需与 Doris 日聚合保持一致。
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

    /**
     * 构建业务对象或响应数据。
     *
     * @param rows rows 参数，用于 buildTrendFromDoris 流程中的校验、计算或对象转换。
     * @param since since 参数，用于 buildTrendFromDoris 流程中的校验、计算或对象转换。
     * @param today today 参数，用于 buildTrendFromDoris 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private List<TrendPointDTO> buildTrendFromDoris(List<DailyStatsDTO> rows, LocalDate since, LocalDate today) {
        // 准备本次处理所需的上下文和中间变量。
        Map<LocalDate, long[]> byDate = new LinkedHashMap<>();
        LocalDate cursor = since;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 构建业务对象或响应数据。
     *
     * @param rows rows 参数，用于 buildTopCanvasesFromDoris 流程中的校验、计算或对象转换。
     * @param Long long 参数，用于 buildTopCanvasesFromDoris 流程中的校验、计算或对象转换。
     * @param canvasById 业务对象 ID，用于定位具体记录。
     * @return 返回组装或转换后的结果对象。
     */
    private List<TopCanvasDTO> buildTopCanvasesFromDoris(List<DailyStatsDTO> rows, Map<Long, CanvasDO> canvasById) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
                    // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 构建业务对象或响应数据。
     *
     * @param totalExecutions total executions 参数，用于 buildAttentionItemsFromDoris 流程中的校验、计算或对象转换。
     * @param failedExecutions failed executions 参数，用于 buildAttentionItemsFromDoris 流程中的校验、计算或对象转换。
     * @param rows rows 参数，用于 buildAttentionItemsFromDoris 流程中的校验、计算或对象转换。
     * @param canvasById 业务对象 ID，用于定位具体记录。
     * @return 返回组装或转换后的结果对象。
     */
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
                        // 关注项只取失败最多的画布，避免首页被大量低价值提醒淹没。
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

    /**
     * 构建业务对象或响应数据。
     *
     * @param executions executions 参数，用于 buildTrend 流程中的校验、计算或对象转换。
     * @param since since 参数，用于 buildTrend 流程中的校验、计算或对象转换。
     * @param today today 参数，用于 buildTrend 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private List<TrendPointDTO> buildTrend(List<CanvasExecutionDO> executions, LocalDate since, LocalDate today) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return trend;
    }

    /**
     * 构建业务对象或响应数据。
     *
     * @param executions executions 参数，用于 buildTopCanvases 流程中的校验、计算或对象转换。
     * @param Long long 参数，用于 buildTopCanvases 流程中的校验、计算或对象转换。
     * @param canvasById 业务对象 ID，用于定位具体记录。
     * @return 返回组装或转换后的结果对象。
     */
    private List<TopCanvasDTO> buildTopCanvases(List<CanvasExecutionDO> executions, Map<Long, CanvasDO> canvasById) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
                    // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 构建业务对象或响应数据。
     *
     * @param totalExecutions total executions 参数，用于 buildAttentionItems 流程中的校验、计算或对象转换。
     * @param failedExecutions failed executions 参数，用于 buildAttentionItems 流程中的校验、计算或对象转换。
     * @param executions executions 参数，用于 buildAttentionItems 流程中的校验、计算或对象转换。
     * @param canvasById 业务对象 ID，用于定位具体记录。
     * @return 返回组装或转换后的结果对象。
     */
    private List<AttentionItemDTO> buildAttentionItems(
            long totalExecutions,
            long failedExecutions,
            List<CanvasExecutionDO> executions,
            Map<Long, CanvasDO> canvasById
    ) {
        List<AttentionItemDTO> items = new ArrayList<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (totalExecutions == 0) {
            items.add(noRecentExecutionsItem());
        }
        if (failedExecutions > 0) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return items;
    }

    /**
     * 执行 noRecentExecutionsItem 流程，围绕 no recent executions item 完成校验、计算或结果组装。
     *
     * @return 返回 noRecentExecutionsItem 流程生成的业务结果。
     */
    private AttentionItemDTO noRecentExecutionsItem() {
        return new AttentionItemDTO(
                0L,
                "全部旅程",
                "NO_RECENT_EXECUTIONS",
                "最近暂无执行记录",
                "info"
        );
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param canvas canvas 参数，用于 canvasName 流程中的校验、计算或对象转换。
     * @param rows rows 参数，用于 canvasName 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private String canvasName(Long canvasId, CanvasDO canvas, List<DailyStatsDTO> rows) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (canvas != null && canvas.getName() != null && !canvas.getName().isBlank()) {
            return canvas.getName();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return rows.stream()
                .map(DailyStatsDTO::canvasName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(String.valueOf(canvasId));
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param execution execution 参数，用于 hasStatus 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回布尔判断结果。
     */
    private boolean hasStatus(CanvasExecutionDO execution, ExecutionStatus status) {
        return Objects.equals(execution.getStatus(), status.getCode());
    }

    /**
     * 执行 formatRate 流程，围绕 format rate 完成校验、计算或结果组装。
     *
     * @param successExecutions success executions 参数，用于 formatRate 流程中的校验、计算或对象转换。
     * @param totalExecutions total executions 参数，用于 formatRate 流程中的校验、计算或对象转换。
     * @return 返回 format rate 生成的文本或业务键。
     */
    private String formatRate(long successExecutions, long totalExecutions) {
        if (totalExecutions <= 0) {
            return "0%";
        }
        double rate = successExecutions * 100.0 / totalExecutions;
        return String.format("%.1f%%", rate);
    }

    /**
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private long value(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * HomeOverviewDTO record.
     * @param range 统计时间范围.
     * @param summary 汇总指标.
     * @param trend 每日执行趋势.
     * @param topCanvases 执行量靠前的画布.
     * @param attentionItems 首页关注项.
     */
    public record HomeOverviewDTO(
        RangeDTO range,
        SummaryDTO summary,
        List<TrendPointDTO> trend,
        List<TopCanvasDTO> topCanvases,
        List<AttentionItemDTO> attentionItems
    ) {
    }

    /**
     * RangeDTO record.
     * @param days 统计天数.
     * @param since 起始日期（含）.
     * @param until 截止日期（含）.
     */
    public record RangeDTO(
        int days,
        String since,
        String until
    ) {
    }

    /**
     * SummaryDTO record.
     * @param publishedCanvasCount 已发布画布数量.
     * @param totalExecutions 执行总次数.
     * @param uniqueUsers 去重用户数.
     * @param failedExecutions 失败执行次数.
     * @param successRate 成功率展示值.
     */
    public record SummaryDTO(
        long publishedCanvasCount,
        long totalExecutions,
        long uniqueUsers,
        long failedExecutions,
        String successRate
    ) {
    }

    /**
     * TrendPointDTO record.
     * @param date 日期.
     * @param total 当日执行次数.
     * @param failed 当日失败次数.
     */
    public record TrendPointDTO(
        String date,
        long total,
        long failed
    ) {
    }

    /**
     * TopCanvasDTO record.
     * @param canvasId 画布 ID.
     * @param name 画布名称.
     * @param total 执行总次数.
     * @param uniqueUsers 去重用户数.
     * @param successRate 成功率展示值.
     * @param failed 失败执行次数.
     */
    public record TopCanvasDTO(
        Long canvasId,
        String name,
        long total,
        long uniqueUsers,
        String successRate,
        long failed
    ) {
    }

    /**
     * AttentionItemDTO record.
     * @param canvasId 关联画布 ID.
     * @param name 关联名称.
     * @param type 提醒类型.
     * @param message 提醒文案.
     * @param severity 严重级别.
     */
    public record AttentionItemDTO(
        Long canvasId,
        String name,
        String type,
        String message,
        String severity
    ) {
    }
}
