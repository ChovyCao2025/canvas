package org.chovy.canvas.engine.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.infra.cache.CanvasConfigCache;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 画布定时调度服务（设计文档 18.1 节）。
 *
 * 发布时注册 SCHEDULED_TRIGGER 节点的调度任务；
 * 下线时取消调度任务。
 * 使用 Spring TaskScheduler（底层 ScheduledExecutorService）管理任务生命周期。
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class CanvasSchedulerService {

    private final TaskScheduler          taskScheduler;
    private final CanvasMapper           canvasMapper;
    private final CanvasConfigCache      configCache;
    private final CanvasExecutionService executionService;

    /** 已注册的调度任务（canvasId:nodeId → ScheduledFuture） */
    private final Map<String, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    // ── 注册 ─────────────────────────────────────────────────────

    public void registerScheduledTriggers(Long canvasId, DagGraph graph) {
        for (String nodeId : graph.entryNodes()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || !"SCHEDULED_TRIGGER".equals(node.getType())) continue;

            Map<String, Object> cfg    = node.getConfig();
            String scheduleType        = (String) cfg.getOrDefault("scheduleType", "CRON");
            String taskKey             = canvasId + ":" + nodeId;

            // 取消旧任务（如重复发布）
            cancelTask(taskKey);

            Runnable job = () -> triggerForAllUsers(canvasId, nodeId, node);

            if ("CRON".equals(scheduleType)) {
                String cron     = (String) cfg.get("cronExpression");
                String timezone = (String) cfg.getOrDefault("timezone", "Asia/Shanghai");
                if (cron == null || cron.isBlank()) continue;
                ScheduledFuture<?> future = taskScheduler.schedule(
                        job, new CronTrigger(cron, TimeZone.getTimeZone(timezone)));
                activeTasks.put(taskKey, future);
                log.info("[SCHEDULER] 注册 CRON 任务 canvasId={} nodeId={} cron={}", canvasId, nodeId, cron);
            } else if ("ONCE".equals(scheduleType)) {
                String triggerTimeStr = (String) cfg.get("triggerTime");
                if (triggerTimeStr == null) continue;
                LocalDateTime ldt = LocalDateTime.parse(triggerTimeStr);
                Instant instant   = ldt.atZone(ZoneId.of("Asia/Shanghai")).toInstant();
                ScheduledFuture<?> future = taskScheduler.schedule(job, instant);
                activeTasks.put(taskKey, future);
                log.info("[SCHEDULER] 注册 ONCE 任务 canvasId={} nodeId={} at={}", canvasId, nodeId, ldt);
            }
        }
    }

    // ── 注销 ─────────────────────────────────────────────────────

    public void cancelScheduledTriggers(Long canvasId, DagGraph graph) {
        for (String nodeId : graph.entryNodes()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || !"SCHEDULED_TRIGGER".equals(node.getType())) continue;
            cancelTask(canvasId + ":" + nodeId);
        }
    }

    @PreDestroy
    public void cancelAll() {
        activeTasks.values().forEach(f -> f.cancel(false));
        log.info("[SCHEDULER] 所有定时任务已取消");
    }

    // ── 内部 ─────────────────────────────────────────────────────

    private void cancelTask(String taskKey) {
        ScheduledFuture<?> old = activeTasks.remove(taskKey);
        if (old != null) old.cancel(false);
    }

    @SuppressWarnings("unchecked")
    private void triggerForAllUsers(Long canvasId, String nodeId, DagParser.CanvasNode node) {
        Map<String, Object> cfg  = node.getConfig();
        Map<String, Object> src  = (Map<String, Object>) cfg.getOrDefault("userSource", Map.of());
        String sourceType        = (String) src.getOrDefault("type", "USER_LIST");
        List<String> userIds     = resolveUserIds(sourceType, src);

        log.info("[SCHEDULER] 定时触发 canvasId={} 用户数={}", canvasId, userIds.size());

        // 分页并发触发（每个用户独立执行）
        for (String userId : userIds) {
            executionService.trigger(
                    canvasId, userId, "SCHEDULED",
                    "SCHEDULED_TRIGGER", null,
                    Map.of(), java.util.UUID.randomUUID().toString(), false)
                    .subscribe(
                            null,
                            e -> log.warn("[SCHEDULER] 用户触发失败 userId={}: {}", userId, e.getMessage())
                    );
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveUserIds(String sourceType, Map<String, Object> src) {
        return switch (sourceType) {
            case "USER_LIST" -> (List<String>) src.getOrDefault("userIds", List.of());
            // TAGGER_GROUP / USER_API：Phase 9 完善，此处返回空列表
            default -> {
                log.warn("[SCHEDULER] userSource type={} 暂不支持，需对接外部系统", sourceType);
                yield List.of();
            }
        };
    }
}
