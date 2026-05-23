package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.schedule.ScheduleKey;
import org.chovy.canvas.engine.schedule.ScheduleRegistrar;
import org.chovy.canvas.engine.schedule.ScheduleRegistration;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 画布定时调度服务（设计文档 18.1 节）。
 *
 * 发布时注册 SCHEDULED_TRIGGER 节点的调度任务；
 * 下线时取消调度任务。
 * 默认本地实现由 LocalTaskScheduleRegistrar 提供，生产环境可替换 ScheduleRegistrar Bean。
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class CanvasSchedulerService {

    private final CanvasExecutionService executionService;
    private final ScheduleRegistrar      scheduleRegistrar;

    @Autowired
    public CanvasSchedulerService(org.springframework.scheduling.TaskScheduler taskScheduler,
                                  org.chovy.canvas.domain.canvas.CanvasMapper canvasMapper,
                                  org.chovy.canvas.infra.cache.CanvasConfigCache configCache,
                                  CanvasExecutionService executionService) {
        this.executionService = executionService;
        this.scheduleRegistrar = new LegacyTaskSchedulerRegistrar(taskScheduler);
    }


    @org.springframework.beans.factory.annotation.Value("${canvas.integration.tagger-service-url}")
    private String taggerUrl;
    @org.springframework.beans.factory.annotation.Value("${canvas.integration.api-call-base-url}")
    private String apiCallUrl;
    @org.springframework.beans.factory.annotation.Value("${canvas.scheduler.jitter-max-ms:300000}")
    private long jitterMaxMs;

    // WebClient 懒建，避免循环依赖
    private org.springframework.web.reactive.function.client.WebClient taggerClient;
    private org.springframework.web.reactive.function.client.WebClient apiCallClient;

    @jakarta.annotation.PostConstruct
    void initClients() {
        taggerClient  = org.springframework.web.reactive.function.client.WebClient.builder().baseUrl(taggerUrl).build();
        apiCallClient = org.springframework.web.reactive.function.client.WebClient.builder().baseUrl(apiCallUrl).build();
    }

    /** 等待 jitter 后触发的回调（canvasId:nodeId → pending subscriptions） */
    private final Map<String, PendingJitterGroup> pendingJitterTasks = new ConcurrentHashMap<>();
    private final Object lifecycleLock = new Object();
    private boolean closed;

    // ── 注册 ─────────────────────────────────────────────────────

    public void registerScheduledTriggers(Long canvasId, DagGraph graph) {
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || !NodeType.SCHEDULED_TRIGGER.equals(node.getType())) continue;

            Map<String, Object> cfg = new java.util.HashMap<>();
            if (node.getBizConfig() != null) cfg.putAll(node.getBizConfig());
            if (node.getConfig()    != null) cfg.putAll(node.getConfig());

            ScheduleKey scheduleKey = scheduleKey(canvasId, nodeId);
            cancelScheduledTrigger(scheduleKey);

            String cronExpr       = (String) cfg.get("cronExpression");
            String triggerTimeStr = (String) cfg.get("triggerTime");
            String timezone       = (String) cfg.getOrDefault("timezone", "Asia/Shanghai");
            if ((cronExpr == null || cronExpr.isBlank()) && (triggerTimeStr == null || triggerTimeStr.isBlank())) {
                log.warn("[SCHEDULER] 跳过未配置时间表达式的定时节点 canvasId={} nodeId={}", canvasId, nodeId);
                continue;
            }

            PendingJitterGroup group = createPendingJitterGroup(scheduleKey.id());
            if (group == null) {
                continue;
            }

            try {
                scheduleRegistrar.register(buildRegistration(
                        scheduleKey, canvasId, nodeId, cfg, timezone, cronExpr, triggerTimeStr, group));
                if (cronExpr != null && !cronExpr.isBlank()) {
                    log.info("[SCHEDULER] 注册 CRON 任务 canvasId={} nodeId={} cron={}", canvasId, nodeId, cronExpr);
                } else if (triggerTimeStr != null) {
                    log.info("[SCHEDULER] 注册 ONCE 任务 canvasId={} nodeId={} at={}",
                            canvasId, nodeId, LocalDateTime.parse(triggerTimeStr));
                }
            } catch (RuntimeException e) {
                removePendingJitterGroup(scheduleKey.id(), group);
                throw e;
            }
        }
    }

    // ── 注销 ─────────────────────────────────────────────────────

    public void cancelScheduledTriggers(Long canvasId, DagGraph graph) {
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || !NodeType.SCHEDULED_TRIGGER.equals(node.getType())) continue;
            cancelScheduledTrigger(scheduleKey(canvasId, nodeId));
        }
    }

    @PreDestroy
    public void cancelAll() {
        List<PendingJitterGroup> groups;
        List<ScheduleKey> scheduleKeys;
        synchronized (lifecycleLock) {
            closed = true;
            groups = new ArrayList<>(pendingJitterTasks.values());
            scheduleKeys = pendingJitterTasks.keySet().stream()
                    .map(taskKey -> new ScheduleKey("canvas", taskKey))
                    .toList();
            groups.forEach(PendingJitterGroup::terminate);
            pendingJitterTasks.clear();
        }
        scheduleKeys.forEach(scheduleRegistrar::unregister);
        groups.forEach(PendingJitterGroup::dispose);
        log.info("[SCHEDULER] 所有定时任务已取消");
    }

    // ── 内部 ─────────────────────────────────────────────────────

    void cancelScheduledTrigger(ScheduleKey key) {
        PendingJitterGroup pending = null;
        synchronized (lifecycleLock) {
            pending = pendingJitterTasks.remove(key.id());
            if (pending != null) pending.terminate();
        }
        scheduleRegistrar.unregister(key);
        if (pending != null) pending.dispose();
    }

    /**
     * Backward-compatible test hook for legacy callers.
     */
    void cancelTask(String taskKey) {
        cancelScheduledTrigger(new ScheduleKey("canvas", taskKey));
    }

    @SuppressWarnings("unchecked")
    private void triggerForAllUsers(Long canvasId, String nodeId, Map<String, Object> cfg, PendingJitterGroup group) {
        Map<String, Object> src = (Map<String, Object>) cfg.getOrDefault("userSource", Map.of());
        String sourceType       = (String) src.getOrDefault("type", "USER_LIST");
        List<String> userIds    = resolveUserIds(sourceType, src);

        log.info("[SCHEDULER] 定时触发 canvasId={} 用户数={}", canvasId, userIds.size());

        for (String userId : userIds) {
            scheduleTriggerWithJitter(group, canvasId, userId, calcJitter(jitterMaxMs));
        }
    }

    PendingJitterGroup createPendingJitterGroup(String taskKey) {
        synchronized (lifecycleLock) {
            if (closed) {
                return null;
            }
            PendingJitterGroup group = new PendingJitterGroup(taskKey);
            pendingJitterTasks.put(taskKey, group);
            return group;
        }
    }

    boolean hasPendingJitterGroup(String taskKey) {
        synchronized (lifecycleLock) {
            return pendingJitterTasks.containsKey(taskKey);
        }
    }

    boolean hasActiveTask(String taskKey) {
        if (scheduleRegistrar instanceof TaskAwareScheduleRegistrar aware) {
            return hasPendingJitterGroup(taskKey) && aware.hasTask(new ScheduleKey("canvas", taskKey));
        }
        return hasPendingJitterGroup(taskKey);
    }

    boolean isCurrentPendingJitterGroup(String taskKey, PendingJitterGroup group) {
        synchronized (lifecycleLock) {
            return pendingJitterTasks.get(taskKey) == group;
        }
    }

    void removePendingJitterGroup(String taskKey, PendingJitterGroup group) {
        boolean removed = false;
        synchronized (lifecycleLock) {
            if (pendingJitterTasks.remove(taskKey, group)) {
                group.terminate();
                removed = true;
            }
        }
        if (removed) group.dispose();
    }

    void cleanupOneShotTask(ScheduleKey scheduleKey, PendingJitterGroup group) {
        boolean removed = false;
        synchronized (lifecycleLock) {
            if (pendingJitterTasks.remove(scheduleKey.id(), group)) {
                group.terminate();
                removed = true;
            }
        }
        scheduleRegistrar.unregister(scheduleKey);
        if (removed) group.dispose();
    }

    private ScheduleRegistration buildRegistration(ScheduleKey scheduleKey,
                                                   Long canvasId,
                                                   String nodeId,
                                                   Map<String, Object> cfg,
                                                   String timezone,
                                                   String cronExpr,
                                                   String triggerTimeStr,
                                                   PendingJitterGroup group) {
        Runnable callback;
        LocalDateTime triggerTime = null;
        if (cronExpr != null && !cronExpr.isBlank()) {
            callback = () -> triggerForAllUsers(canvasId, nodeId, cfg, group);
        } else {
            triggerTime = LocalDateTime.parse(triggerTimeStr);
            callback = () -> {
                try {
                    triggerForAllUsers(canvasId, nodeId, cfg, group);
                } finally {
                    group.closeWhenIdle(() -> cleanupOneShotTask(scheduleKey, group));
                }
            };
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("canvasId", canvasId);
        metadata.put("nodeId", nodeId);
        metadata.put("timezone", timezone);
        if (cronExpr != null && !cronExpr.isBlank()) {
            metadata.put("cronExpression", cronExpr);
        }
        if (triggerTimeStr != null && !triggerTimeStr.isBlank()) {
            metadata.put("triggerTime", triggerTimeStr);
        }
        return new ScheduleRegistration(
                scheduleKey,
                cronExpr,
                triggerTime,
                timezone,
                callback,
                metadata
        );
    }

    private ScheduleKey scheduleKey(Long canvasId, String nodeId) {
        return new ScheduleKey("canvas", canvasId + ":" + nodeId);
    }

    Disposable scheduleTriggerWithJitter(PendingJitterGroup group, Long canvasId, String userId, Duration jitter) {
        if (jitter.isZero() || jitter.isNegative()) {
            if (!group.canScheduleImmediate()) {
                return Disposables.disposed();
            }
            dispatchScheduledTrigger(group, canvasId, userId);
            return Disposables.disposed();
        }

        Disposable.Swap pending = Disposables.swap();
        if (!group.add(pending)) {
            return Disposables.disposed();
        }

        pending.update(Mono.delay(jitter)
                .doFinally(signalType -> group.remove(pending))
                .subscribe(
                        ignored -> dispatchScheduledTrigger(group, canvasId, userId),
                        e -> log.warn("[SCHEDULER] jitter 调度失败 taskKey={} userId={}: {}", group.taskKey, userId, e.getMessage())
                ));
        return pending;
    }

    void dispatchScheduledTrigger(PendingJitterGroup group, Long canvasId, String userId) {
        if (group.isTerminated()) {
            return;
        }
        executionService.trigger(
                        canvasId, userId, TriggerType.SCHEDULED,
                        NodeType.SCHEDULED_TRIGGER, null,
                        Map.of(), java.util.UUID.randomUUID().toString(), false)
                .subscribe(
                        null,
                        e -> log.warn("[SCHEDULER] 用户触发失败 userId={}: {}", userId, e.getMessage())
                );
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
                            .bodyValue(java.util.Map.of(
                                    MapFieldKeys.API_KEY, apiKey,
                                    MapFieldKeys.PARAMS, src.getOrDefault(MapFieldKeys.PARAMS, java.util.Map.of())))
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

    /** 生成 [0, jitterMaxMs) 区间内的随机延迟；jitterMaxMs=0 时返回 ZERO。可静态调用，便于单测。 */
    static Duration calcJitter(long jitterMaxMs) {
        if (jitterMaxMs <= 0) return Duration.ZERO;
        return Duration.ofMillis(java.util.concurrent.ThreadLocalRandom.current().nextLong(0, jitterMaxMs));
    }

    static final class PendingJitterGroup {
        private final String taskKey;
        private final Set<Disposable> pending = ConcurrentHashMap.newKeySet();
        private final AtomicInteger size = new AtomicInteger();
        private final AtomicBoolean closeWhenIdle = new AtomicBoolean(false);
        private final AtomicBoolean terminated = new AtomicBoolean(false);
        private final AtomicBoolean cleanupTriggered = new AtomicBoolean(false);
        private volatile Runnable onIdle;

        private PendingJitterGroup(String taskKey) {
            this.taskKey = taskKey;
        }

        boolean add(Disposable disposable) {
            if (terminated.get() || closeWhenIdle.get()) {
                return false;
            }
            if (!pending.add(disposable)) {
                return false;
            }
            size.incrementAndGet();
            if (terminated.get() || closeWhenIdle.get()) {
                if (pending.remove(disposable)) {
                    size.decrementAndGet();
                }
                disposable.dispose();
                return false;
            }
            return true;
        }

        void remove(Disposable disposable) {
            if (pending.remove(disposable)) {
                size.decrementAndGet();
                cleanupIfIdle();
            }
        }

        void closeWhenIdle(Runnable onIdle) {
            this.onIdle = onIdle;
            closeWhenIdle.set(true);
            cleanupIfIdle();
        }

        private void cleanupIfIdle() {
            Runnable cleanup = onIdle;
            if (closeWhenIdle.get() && size.get() == 0 && cleanup != null
                    && cleanupTriggered.compareAndSet(false, true)) {
                cleanup.run();
            }
        }

        void terminate() {
            terminated.set(true);
        }

        boolean isTerminated() {
            return terminated.get();
        }

        boolean canScheduleImmediate() {
            return !terminated.get() && !closeWhenIdle.get();
        }

        void dispose() {
            terminated.set(true);
            for (Disposable disposable : pending) {
                disposable.dispose();
            }
            pending.clear();
            size.set(0);
        }
    }

    private interface TaskAwareScheduleRegistrar {
        boolean hasTask(ScheduleKey key);
    }

    private static final class LegacyTaskSchedulerRegistrar implements ScheduleRegistrar, TaskAwareScheduleRegistrar {
        private final org.springframework.scheduling.TaskScheduler taskScheduler;
        private final Map<ScheduleKey, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
        private final Set<ScheduleKey> cancelledDuringRegistration = ConcurrentHashMap.newKeySet();

        private LegacyTaskSchedulerRegistrar(org.springframework.scheduling.TaskScheduler taskScheduler) {
            this.taskScheduler = taskScheduler;
        }

        @Override
        public void register(ScheduleRegistration registration) {
            unregister(registration.key());
            cancelledDuringRegistration.remove(registration.key());
            ScheduledFuture<?> future = registration.cronExpression() != null && !registration.cronExpression().isBlank()
                    ? taskScheduler.schedule(registration.callback(),
                    new org.springframework.scheduling.support.CronTrigger(registration.cronExpression(),
                            java.util.TimeZone.getTimeZone(registration.timezone())))
                    : taskScheduler.schedule(registration.callback(),
                    registration.triggerTime().atZone(java.time.ZoneId.of(registration.timezone())).toInstant());
            if (future == null) {
                throw new IllegalStateException("Local TaskScheduler returned null for " + registration.key());
            }
            if (cancelledDuringRegistration.remove(registration.key())) {
                future.cancel(false);
                return;
            }
            tasks.put(registration.key(), future);
        }

        @Override
        public void unregister(ScheduleKey key) {
            ScheduledFuture<?> future = tasks.remove(key);
            if (future != null) {
                future.cancel(false);
            } else {
                cancelledDuringRegistration.add(key);
            }
        }

        @Override
        public boolean hasTask(ScheduleKey key) {
            return tasks.containsKey(key);
        }
    }
}
