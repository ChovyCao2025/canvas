package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.ExecutionStatus;
import org.chovy.canvas.domain.execution.CanvasExecution;
import org.chovy.canvas.domain.execution.CanvasExecutionMapper;
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

@RestController
@RequestMapping("/canvas/home")
@RequiredArgsConstructor
public class HomeOverviewController {

    private final CanvasMapper canvasMapper;
    private final CanvasExecutionMapper executionMapper;

    @GetMapping("/overview")
    public Mono<R<HomeOverviewDTO>> overview(@RequestParam(name = "days", defaultValue = "7") int days) {
        int normalizedDays = days <= 0 || days > 30 ? 7 : days;
        return Mono.fromCallable(() -> R.ok(buildOverview(normalizedDays)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private HomeOverviewDTO buildOverview(int days) {
        LocalDate today = LocalDate.now();
        LocalDate since = today.minusDays(days - 1L);
        LocalDateTime sinceTime = since.atStartOfDay();
        LocalDateTime untilTime = today.plusDays(1).atStartOfDay();

        List<Canvas> publishedCanvases = canvasMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .filter(canvas -> CanvasStatusEnum.PUBLISHED.getCode().equals(canvas.getStatus()))
                .toList();
        Map<Long, Canvas> canvasById = publishedCanvases.stream()
                .collect(Collectors.toMap(Canvas::getId, canvas -> canvas, (left, right) -> left, LinkedHashMap::new));

        List<CanvasExecution> executions = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecution>()
                        .ge(CanvasExecution::getCreatedAt, sinceTime)
                        .lt(CanvasExecution::getCreatedAt, untilTime)
        );
        List<CanvasExecution> relevantExecutions = executions.stream()
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
                .map(CanvasExecution::getUserId)
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

    private List<TrendPointDTO> buildTrend(List<CanvasExecution> executions, int days) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, List<CanvasExecution>> byDate = executions.stream()
                .filter(execution -> execution.getCreatedAt() != null)
                .collect(Collectors.groupingBy(execution -> execution.getCreatedAt().toLocalDate()));

        List<TrendPointDTO> trend = new ArrayList<>(days);
        for (int offset = days - 1; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            List<CanvasExecution> dayExecutions = byDate.getOrDefault(date, List.of());
            long failed = dayExecutions.stream()
                    .filter(execution -> hasStatus(execution, ExecutionStatus.FAILED))
                    .count();
            trend.add(new TrendPointDTO(date.toString(), dayExecutions.size(), failed));
        }
        return trend;
    }

    private List<TopCanvasDTO> buildTopCanvases(List<CanvasExecution> executions, Map<Long, Canvas> canvasById) {
        return executions.stream()
                .collect(Collectors.groupingBy(CanvasExecution::getCanvasId))
                .entrySet()
                .stream()
                .map(entry -> {
                    Long canvasId = entry.getKey();
                    List<CanvasExecution> canvasExecutions = entry.getValue();
                    long failed = canvasExecutions.stream()
                            .filter(execution -> hasStatus(execution, ExecutionStatus.FAILED))
                            .count();
                    long success = canvasExecutions.stream()
                            .filter(execution -> hasStatus(execution, ExecutionStatus.SUCCESS))
                            .count();
                    long uniqueUsers = canvasExecutions.stream()
                            .map(CanvasExecution::getUserId)
                            .filter(userId -> userId != null && !userId.isBlank())
                            .distinct()
                            .count();
                    Canvas canvas = canvasById.get(canvasId);
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
            List<CanvasExecution> executions,
            Map<Long, Canvas> canvasById
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
                    .collect(Collectors.groupingBy(CanvasExecution::getCanvasId, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .ifPresent(entry -> {
                        Long canvasId = entry.getKey();
                        Canvas canvas = canvasById.get(canvasId);
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

    private boolean hasStatus(CanvasExecution execution, ExecutionStatus status) {
        return execution.getStatus() != null && execution.getStatus() == status.getCode();
    }

    private String formatRate(long successExecutions, long totalExecutions) {
        if (totalExecutions <= 0) {
            return "0%";
        }
        double rate = successExecutions * 100.0 / totalExecutions;
        return String.format("%.1f%%", rate);
    }

    public record HomeOverviewDTO(
            RangeDTO range,
            SummaryDTO summary,
            List<TrendPointDTO> trend,
            List<TopCanvasDTO> topCanvases,
            List<AttentionItemDTO> attentionItems
    ) {
    }

    public record RangeDTO(int days, String since, String until) {
    }

    public record SummaryDTO(
            long publishedCanvasCount,
            long totalExecutions,
            long uniqueUsers,
            long failedExecutions,
            String successRate
    ) {
    }

    public record TrendPointDTO(String date, long total, long failed) {
    }

    public record TopCanvasDTO(Long canvasId, String name, long total, long uniqueUsers, String successRate, long failed) {
    }

    public record AttentionItemDTO(Long canvasId, String name, String type, String message, String severity) {
    }
}
