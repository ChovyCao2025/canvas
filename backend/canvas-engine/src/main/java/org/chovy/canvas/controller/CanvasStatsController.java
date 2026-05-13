package org.chovy.canvas.controller;

import org.chovy.canvas.common.R;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.domain.execution.CanvasExecutionMapper;
import org.chovy.canvas.domain.execution.CanvasExecution;
import org.chovy.canvas.domain.execution.CanvasExecutionTraceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.*;

/**
 * 活动效果分析 API（设计文档第二十一章 21.3节）。
 *
 * GET /canvas/{id}/stats  整体执行统计（时间范围）
 * GET /canvas/{id}/funnel 节点漏斗转化
 * GET /canvas/{id}/trend  每日执行量趋势
 */
@RestController
@RequestMapping("/canvas/{id}")
@RequiredArgsConstructor
public class CanvasStatsController {

    private final CanvasExecutionMapper      executionMapper;
    private final CanvasExecutionTraceMapper traceMapper;

    /** 某次执行的所有节点轨迹（前端执行轨迹可视化，14.2节） */
    @GetMapping("/execution/{executionId}/trace")
    public Mono<R<List<Map<String, Object>>>> getTrace(@PathVariable String executionId) {
        return Mono.fromCallable(() -> {
            List<org.chovy.canvas.domain.execution.CanvasExecutionTrace> traces =
                    traceMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                    org.chovy.canvas.domain.execution.CanvasExecutionTrace>()
                                    .eq(org.chovy.canvas.domain.execution.CanvasExecutionTrace::getExecutionId,
                                            executionId)
                                    .orderByAsc(org.chovy.canvas.domain.execution.CanvasExecutionTrace::getStartedAt));
            return traces.stream().map(t -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("nodeId",    t.getNodeId());
                m.put("nodeType",  t.getNodeType());
                m.put("nodeName",  t.getNodeName());
                m.put("status",    t.getStatus());
                m.put("errorMsg",  t.getErrorMsg());
                if (t.getStartedAt() != null && t.getFinishedAt() != null) {
                    m.put("durationMs",
                            java.time.Duration.between(t.getStartedAt(), t.getFinishedAt()).toMillis());
                }
                return m;
            }).collect(java.util.stream.Collectors.toList());
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).map(R::ok);
    }

    /** 画布最近 N 次执行记录（用于前端执行轨迹选择器） */
    @GetMapping("/executions")
    public Mono<R<List<Map<String, Object>>>> recentExecutions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> {
            List<org.chovy.canvas.domain.execution.CanvasExecution> execs =
                    executionMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                    org.chovy.canvas.domain.execution.CanvasExecution>()
                                    .eq(org.chovy.canvas.domain.execution.CanvasExecution::getCanvasId, id)
                                    .orderByDesc(org.chovy.canvas.domain.execution.CanvasExecution::getCreatedAt)
                                    .last("LIMIT " + Math.min(size, 100)));
            return execs.stream().map(e -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id",          e.getId());
                m.put("triggerType", e.getTriggerType());
                m.put("status",      e.getStatus());
                m.put("userId",      e.getUserId());
                m.put("createdAt",   e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
                return m;
            }).collect(java.util.stream.Collectors.toList());
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).map(R::ok);
    }

    /** 整体执行统计 */
    @GetMapping("/stats")
    public Mono<R<Map<String, Object>>> stats(
            @PathVariable Long id,
            @RequestParam(defaultValue = "7") int days) {

        return Mono.fromCallable(() -> {
            LocalDate since = LocalDate.now().minusDays(days);

            List<CanvasExecution> executions = executionMapper.selectList(
                    new LambdaQueryWrapper<CanvasExecution>()
                            .eq(CanvasExecution::getCanvasId, id)
                            .ge(CanvasExecution::getCreatedAt, since.atStartOfDay()));

            long total   = executions.size();
            long success = executions.stream().filter(e -> e.getStatus() == 2).count();
            long failed  = executions.stream().filter(e -> e.getStatus() == 3).count();
            long paused  = executions.stream().filter(e -> e.getStatus() == 1).count();

            Set<String> uniqueUsers = new HashSet<>();
            executions.stream()
                    .filter(e -> e.getUserId() != null)
                    .forEach(e -> uniqueUsers.add(e.getUserId()));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("canvasId",   id);
            result.put("days",       days);
            result.put("total",      total);
            result.put("success",    success);
            result.put("failed",     failed);
            result.put("paused",     paused);
            result.put("successRate", total > 0 ? String.format("%.1f%%", success * 100.0 / total) : "0%");
            result.put("uniqueUsers", uniqueUsers.size());
            return result;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 节点漏斗（设计文档 21.3节）：聚合每个节点的进入/成功/失败/跳过次数。
     * 前端按此数据在画布上叠加漏斗可视化。
     */
    @GetMapping("/funnel")
    public Mono<R<List<Map<String, Object>>>> funnel(@PathVariable Long id) {
        return Mono.fromCallable(() ->
                traceMapper.selectFunnelByCanvasId(id)
        ).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /** 每日执行量趋势（按天聚合） */
    @GetMapping("/trend")
    public Mono<R<List<Map<String, Object>>>> trend(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int days) {

        return Mono.fromCallable(() -> {
            List<CanvasExecution> executions = executionMapper.selectList(
                    new LambdaQueryWrapper<CanvasExecution>()
                            .eq(CanvasExecution::getCanvasId, id)
                            .ge(CanvasExecution::getCreatedAt,
                                    LocalDate.now().minusDays(days).atStartOfDay())
                            .orderByAsc(CanvasExecution::getCreatedAt));

            // 按日聚合
            Map<String, Long> byDate = new LinkedHashMap<>();
            for (CanvasExecution e : executions) {
                if (e.getCreatedAt() == null) continue;
                String date = e.getCreatedAt().toLocalDate().toString();
                byDate.merge(date, 1L, Long::sum);
            }

            List<Map<String, Object>> trend = new ArrayList<>();
            byDate.forEach((date, count) -> {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("date", date);
                point.put("count", count);
                trend.add(point);
            });
            return trend;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }
}
