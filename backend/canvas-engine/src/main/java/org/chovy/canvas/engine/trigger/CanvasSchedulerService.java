package org.chovy.canvas.engine.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.constant.TriggerType;
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

    @org.springframework.beans.factory.annotation.Value("${canvas.integration.tagger-service-url}")
    private String taggerUrl;
    @org.springframework.beans.factory.annotation.Value("${canvas.integration.api-call-base-url}")
    private String apiCallUrl;

    // WebClient 懒建，避免循环依赖
    private org.springframework.web.reactive.function.client.WebClient taggerClient;
    private org.springframework.web.reactive.function.client.WebClient apiCallClient;

    @jakarta.annotation.PostConstruct
    void initClients() {
        taggerClient  = org.springframework.web.reactive.function.client.WebClient.builder().baseUrl(taggerUrl).build();
        apiCallClient = org.springframework.web.reactive.function.client.WebClient.builder().baseUrl(apiCallUrl).build();
    }

    /** 已注册的调度任务（canvasId:nodeId → ScheduledFuture） */
    private final Map<String, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    // ── 注册 ─────────────────────────────────────────────────────

    public void registerScheduledTriggers(Long canvasId, DagGraph graph) {
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || !NodeType.SCHEDULED_TRIGGER.equals(node.getType())) continue;

            Map<String, Object> cfg = new java.util.HashMap<>();
            if (node.getBizConfig() != null) cfg.putAll(node.getBizConfig());
            if (node.getConfig()    != null) cfg.putAll(node.getConfig());

            String taskKey = canvasId + ":" + nodeId;
            cancelTask(taskKey);

            Runnable job = () -> triggerForAllUsers(canvasId, nodeId, cfg);

            String cronExpr       = (String) cfg.get("cronExpression");
            String triggerTimeStr = (String) cfg.get("triggerTime");
            String timezone       = (String) cfg.getOrDefault("timezone", "Asia/Shanghai");

            if (cronExpr != null && !cronExpr.isBlank()) {
                ScheduledFuture<?> future = taskScheduler.schedule(
                        job, new CronTrigger(cronExpr, TimeZone.getTimeZone(timezone)));
                activeTasks.put(taskKey, future);
                log.info("[SCHEDULER] 注册 CRON 任务 canvasId={} nodeId={} cron={}", canvasId, nodeId, cronExpr);
            } else if (triggerTimeStr != null) {
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
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || !NodeType.SCHEDULED_TRIGGER.equals(node.getType())) continue;
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
    private void triggerForAllUsers(Long canvasId, String nodeId, Map<String, Object> cfg) {
        Map<String, Object> src = (Map<String, Object>) cfg.getOrDefault("userSource", Map.of());
        String sourceType       = (String) src.getOrDefault("type", "USER_LIST");
        List<String> userIds    = resolveUserIds(sourceType, src);

        log.info("[SCHEDULER] 定时触发 canvasId={} 用户数={}", canvasId, userIds.size());

        for (String userId : userIds) {
            executionService.trigger(
                    canvasId, userId, TriggerType.SCHEDULED,
                    NodeType.SCHEDULED_TRIGGER, null,
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

            case "TAGGER_GROUP" -> {
                // 调用 Tagger 离线用户列表接口获取指定标签的用户 ID 列表
                String tagCode = (String) src.get("tagCode");
                int limit = src.get("limit") instanceof Number n ? n.intValue() : 10000;
                int pageSize = src.get("pageSize") instanceof Number n ? n.intValue() : 1000;
                if (tagCode == null) { log.warn("[SCHEDULER] TAGGER_GROUP 缺少 tagCode"); yield List.of(); }
                try {
                    List<String> result = new java.util.ArrayList<>();
                    int page = 1;
                    while (result.size() < limit) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> resp = taggerClient.get()
                                .uri(u -> u.path("/offline/users")
                                        .queryParam("tagCode", tagCode)
                                        .queryParam("page", page)
                                        .queryParam("size", pageSize).build())
                                .retrieve()
                                .bodyToMono(java.util.Map.class)
                                .block();
                        List<String> batch = resp != null ? (List<String>) resp.getOrDefault("userIds", List.of()) : List.of();
                        result.addAll(batch);
                        if (batch.size() < pageSize) break; // 最后一页
                    }
                    log.info("[SCHEDULER] TAGGER_GROUP tagCode={} 拉取 {} 个用户", tagCode, result.size());
                    yield result.subList(0, Math.min(result.size(), limit));
                } catch (Exception e) {
                    log.error("[SCHEDULER] TAGGER_GROUP 用户列表拉取失败: {}", e.getMessage());
                    yield List.of();
                }
            }

            case "USER_API" -> {
                // 调用自定义接口获取用户列表
                String apiKey = (String) src.get("apiKey");
                if (apiKey == null) { log.warn("[SCHEDULER] USER_API 缺少 apiKey"); yield List.of(); }
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> resp = apiCallClient.post()
                            .uri("/call")
                            .bodyValue(java.util.Map.of("apiKey", apiKey, "params", src.getOrDefault("params", java.util.Map.of())))
                            .retrieve()
                            .bodyToMono(java.util.Map.class)
                            .block();
                    List<String> userIds = resp != null ? (List<String>) resp.getOrDefault("userIds", List.of()) : List.of();
                    log.info("[SCHEDULER] USER_API apiKey={} 返回 {} 个用户", apiKey, userIds.size());
                    yield userIds;
                } catch (Exception e) {
                    log.error("[SCHEDULER] USER_API 用户列表获取失败: {}", e.getMessage());
                    yield List.of();
                }
            }

            default -> {
                log.warn("[SCHEDULER] 未知 userSource type={}", sourceType);
                yield List.of();
            }
        };
    }
}
