package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
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
import java.util.stream.Collectors;

/**
 * 首页概览 HTTP 控制器，根路由为 {@code /canvas/home}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/canvas/home")
@RequiredArgsConstructor
public class HomeOverviewController {

    /** 画布 Mapper，用于统计已发布画布。 */
    private final CanvasMapper canvasMapper;
    /** 执行记录 Mapper，用于统计首页执行数据。 */
    private final CanvasExecutionMapper executionMapper;

    @GetMapping("/overview")
    public Mono<R<HomeOverviewDTO>> overview(@RequestParam(name = "days", defaultValue = "7") int days) {
        int normalizedDays = days <= 0 || days > 30 ? 7 : days;
        return Mono.fromCallable(() -> R.ok(buildOverview(normalizedDays)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 构建、解析或转换 build Overview 相关的业务数据。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param days days 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    private HomeOverviewDTO buildOverview(int days) {
        LocalDate today = LocalDate.now();
        LocalDate since = today.minusDays(days - 1L);
        LocalDateTime sinceTime = since.atStartOfDay();
        LocalDateTime untilTime = today.plusDays(1).atStartOfDay();

        List<CanvasDO> publishedCanvases = canvasMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .filter(canvas -> CanvasStatusEnum.PUBLISHED.getCode().equals(canvas.getStatus()))
                .toList();
        Map<Long, CanvasDO> canvasById = publishedCanvases.stream()
                .collect(Collectors.toMap(CanvasDO::getId, canvas -> canvas, (left, right) -> left, LinkedHashMap::new));

        List<CanvasExecutionDO> executions = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecutionDO>()
                        .ge(CanvasExecutionDO::getCreatedAt, sinceTime)
                        .lt(CanvasExecutionDO::getCreatedAt, untilTime)
        );
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

        List<TrendPointDTO> trend = buildTrend(relevantExecutions, days);
        List<TopCanvasDTO> topCanvases = buildTopCanvases(relevantExecutions, canvasById);
        List<AttentionItemDTO> attentionItems = buildAttentionItems(
                totalExecutions,
                failedExecutions,
                relevantExecutions,
                canvasById
        );

        return new HomeOverviewDTO(
                new RangeDTO(days, since.toString(), today.toString()),
                new SummaryDTO(
                        publishedCanvases.size(),
                        totalExecutions,
                        uniqueUsers,
                        failedExecutions,
                        formatRate(successExecutions, totalExecutions)
                ),
                trend,
                topCanvases,
                attentionItems
        );
    }

    /**
     * 构建、解析或转换 build Trend 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param executions executions 方法执行所需的业务参数
     * @param days days 方法执行所需的业务参数
     * @return 查询、转换或计算得到的结果集合
     */
    private List<TrendPointDTO> buildTrend(List<CanvasExecutionDO> executions, int days) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, List<CanvasExecutionDO>> byDate = executions.stream()
                .filter(execution -> execution.getCreatedAt() != null)
                .collect(Collectors.groupingBy(execution -> execution.getCreatedAt().toLocalDate()));

        List<TrendPointDTO> trend = new ArrayList<>(days);
        for (int offset = days - 1; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            List<CanvasExecutionDO> dayExecutions = byDate.getOrDefault(date, List.of());
            long failed = dayExecutions.stream()
                    .filter(execution -> hasStatus(execution, ExecutionStatus.FAILED))
                    .count();
            trend.add(new TrendPointDTO(date.toString(), dayExecutions.size(), failed));
        }
        return trend;
    }

    /**
     * 构建、解析或转换 build Top Canvases 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param executions executions 方法执行所需的业务参数
     * @param canvasById canvasById 对应的业务主键或标识
     * @return 查询、转换或计算得到的结果集合
     */
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

    /**
     * 构建、解析或转换 build Attention Items 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param totalExecutions totalExecutions 方法执行所需的业务参数
     * @param failedExecutions failedExecutions 方法执行所需的业务参数
     * @param executions executions 方法执行所需的业务参数
     * @param canvasById canvasById 对应的业务主键或标识
     * @return 查询、转换或计算得到的结果集合
     */
    private List<AttentionItemDTO> buildAttentionItems(
            long totalExecutions,
            long failedExecutions,
            List<CanvasExecutionDO> executions,
            Map<Long, CanvasDO> canvasById
    ) {
        List<AttentionItemDTO> items = new ArrayList<>();
        if (totalExecutions == 0) {
            items.add(new AttentionItemDTO(
                    0L,
                    "全部旅程",
                    "NO_RECENT_EXECUTIONS",
                    "最近暂无执行记录",
                    "info"
            ));
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

    /**
     * 判断 has Status 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param execution execution 方法执行所需的业务参数
     * @param status status 状态值或状态筛选条件
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean hasStatus(CanvasExecutionDO execution, ExecutionStatus status) {
        return execution.getStatus() != null && execution.getStatus() == status.getCode();
    }

    /**
     * 执行 format Rate 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param successExecutions successExecutions 方法执行所需的业务参数
     * @param totalExecutions totalExecutions 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String formatRate(long successExecutions, long totalExecutions) {
        if (totalExecutions <= 0) {
            return "0%";
        }
        double rate = successExecutions * 100.0 / totalExecutions;
        return String.format("%.1f%%", rate);
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
