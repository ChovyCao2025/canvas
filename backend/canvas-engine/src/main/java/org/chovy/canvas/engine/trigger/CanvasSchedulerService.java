package org.chovy.canvas.engine.trigger;

import jakarta.annotation.PostConstruct;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.reactive.BackgroundSubscriptionRegistry;
import org.chovy.canvas.engine.schedule.ScheduleKey;
import org.chovy.canvas.engine.schedule.ScheduleRegistrar;
import org.chovy.canvas.engine.schedule.ScheduleRegistration;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.chovy.canvas.dal.mapper.CanvasMapper;

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
public class CanvasSchedulerService {

    /** 画布执行服务，用于定时任务到期后触发画布执行。 */
    private final CanvasExecutionService executionService;
    /** 调度注册器。 */
    private final ScheduleRegistrar      scheduleRegistrar;
    /** 后台订阅注册器，用于托管定时触发执行链。 */
    private final BackgroundSubscriptionRegistry backgroundSubscriptions;
    private static final String SCHEDULED_BATCH_USER_PREFIX = "__scheduled_batch__:";

    public CanvasSchedulerService(CanvasExecutionService executionService,
                                  ScheduleRegistrar scheduleRegistrar) {
        this(executionService, scheduleRegistrar, new BackgroundSubscriptionRegistry());
    }

    public CanvasSchedulerService(CanvasExecutionService executionService,
                                  ScheduleRegistrar scheduleRegistrar,
                                  BackgroundSubscriptionRegistry backgroundSubscriptions) {
        this.executionService = executionService;
        this.scheduleRegistrar = scheduleRegistrar;
        this.backgroundSubscriptions = backgroundSubscriptions;
    }

    /** 兼容旧测试和默认本地调度器的构造器，生产可替换 ScheduleRegistrar 实现。 */
    public CanvasSchedulerService(org.springframework.scheduling.TaskScheduler taskScheduler,
                                  org.chovy.canvas.dal.mapper.CanvasMapper canvasMapper,
                                  org.chovy.canvas.infrastructure.cache.CanvasConfigCache configCache,
                                  CanvasExecutionService executionService) {
        this(taskScheduler, canvasMapper, configCache, executionService, new BackgroundSubscriptionRegistry());
    }

    @Autowired
    public CanvasSchedulerService(org.springframework.scheduling.TaskScheduler taskScheduler,
                                  org.chovy.canvas.dal.mapper.CanvasMapper canvasMapper,
                                  org.chovy.canvas.infrastructure.cache.CanvasConfigCache configCache,
                                  CanvasExecutionService executionService,
                                  BackgroundSubscriptionRegistry backgroundSubscriptions) {
        this.executionService = executionService;
        this.scheduleRegistrar = new LegacyTaskSchedulerRegistrar(taskScheduler);
        this.backgroundSubscriptions = backgroundSubscriptions;
    }


    /** Tagger 服务地址。 */
    @Value("${canvas.integration.tagger-service-url}")
    private String taggerUrl;
    /** API_CALL 健康检查或预热地址。 */
    @Value("${canvas.integration.api-call-base-url}")
    private String apiCallUrl;
    /** 定时触发抖动最大毫秒数。 */
    @Value("${canvas.scheduler.jitter-max-ms:300000}")
    private long jitterMaxMs;

    /** Tagger WebClient 懒加载实例，避免构造阶段产生循环依赖。 */
    private org.springframework.web.reactive.function.client.WebClient taggerClient;
    /** API_CALL WebClient 客户端。 */
    private org.springframework.web.reactive.function.client.WebClient apiCallClient;

    /** 在 Bean 初始化完成后创建外部用户来源查询客户端。 */
    @PostConstruct
    void initClients() {
        taggerClient  = org.springframework.web.reactive.function.client.WebClient.builder().baseUrl(taggerUrl).build();
        apiCallClient = org.springframework.web.reactive.function.client.WebClient.builder().baseUrl(apiCallUrl).build();
    }

    /** 带抖动的延迟调度任务分组，避免大量定时触发同时打入执行链路。 */
    private final Map<String, PendingJitterGroup> pendingJitterTasks = new ConcurrentHashMap<>();
    /** 调度生命周期锁，保护注册、取消和关闭过程的并发状态。 */
    private final Object lifecycleLock = new Object();
    /** 调度服务是否已关闭。 */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 注册、调度或初始化 register Scheduled Triggers 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param graph graph 方法执行所需的业务参数
     */
// ── 注册 ─────────────────────────────────────────────────────

    /** 扫描发布态 DAG 中的定时触发节点并注册对应调度任务。 */
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
                // 服务正在关闭时拒绝新调度分组，避免注册后无人清理。
                continue;
            }

            try {
                scheduleRegistrar.register(buildRegistration(
                        scheduleKey, canvasId, nodeId, cfg, timezone, cronExpr, triggerTimeStr, group));
                if (cronExpr != null && !cronExpr.isBlank()) {
                    log.info("[SCHEDULER] 注册 CRON 任务 canvasId={} nodeId={} cron={}", canvasId, nodeId, cronExpr);
                } else if (triggerTimeStr != null) {
                    log.info("[SCHEDULER] 注册 ONCE 任务 canvasId={} nodeId={} at={}",
                            canvasId, nodeId, parseTriggerTime(triggerTimeStr, timezone));
                }
            } catch (RuntimeException e) {
                removePendingJitterGroup(scheduleKey.id(), group);
                throw e;
            }
        }
    }

    /**
     * 判断 cancel Scheduled Triggers 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param graph graph 方法执行所需的业务参数
     */
// ── 注销 ─────────────────────────────────────────────────────

    /** 按画布 DAG 注销全部定时触发节点对应的调度任务。 */
    public void cancelScheduledTriggers(Long canvasId, DagGraph graph) {
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || !NodeType.SCHEDULED_TRIGGER.equals(node.getType())) continue;
            cancelScheduledTrigger(scheduleKey(canvasId, nodeId));
        }
    }

    /** 取消全部已注册调度任务。 */
    @PreDestroy
    public void cancelAll() {
        List<PendingJitterGroup> groups;
        List<ScheduleKey> scheduleKeys;
        synchronized (lifecycleLock) {
            closed.set(true);
            groups = new ArrayList<>(pendingJitterTasks.values());
            scheduleKeys = pendingJitterTasks.keySet().stream()
                    .map(taskKey -> new ScheduleKey("canvas", taskKey))
                    .toList();
            // 先标记终止，阻止并发线程继续追加 jitter 延迟任务。
            groups.forEach(PendingJitterGroup::terminate);
            pendingJitterTasks.clear();
        }
        scheduleKeys.forEach(scheduleRegistrar::unregister);
        groups.forEach(PendingJitterGroup::dispose);
        log.info("[SCHEDULER] 所有定时任务已取消");
    }

    /**
     * 判断 cancel Scheduled Trigger 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     */
// ── 内部 ─────────────────────────────────────────────────────

    /** 注销单个调度任务，并清理其尚未执行的 jitter 延迟任务。 */
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
    /** 兼容旧单测的取消入口，将历史 taskKey 转换为 ScheduleKey。 */
    void cancelTask(String taskKey) {
        cancelScheduledTrigger(new ScheduleKey("canvas", taskKey));
    }

    /** 定时节点到点后只启动一次批处理执行，用户展开交给后续人群节点。 */
    private void triggerForAllUsers(Long canvasId, String nodeId, Map<String, Object> cfg, PendingJitterGroup group) {
        log.info("[SCHEDULER] 定时批处理触发 canvasId={} nodeId={}", canvasId, nodeId);
        scheduleTriggerWithJitter(group, canvasId, scheduledBatchUserId(canvasId, nodeId), Duration.ZERO);
    }

    /** 创建并登记 jitter 分组，服务关闭后不再接受新分组。 */
    PendingJitterGroup createPendingJitterGroup(String taskKey) {
        synchronized (lifecycleLock) {
            if (closed.get()) {
                return null;
            }
            PendingJitterGroup group = new PendingJitterGroup(taskKey);
            pendingJitterTasks.put(taskKey, group);
            return group;
        }
    }

    /** 判断指定任务 key 是否仍存在待执行 jitter 分组。 */
    boolean hasPendingJitterGroup(String taskKey) {
        synchronized (lifecycleLock) {
            return pendingJitterTasks.containsKey(taskKey);
        }
    }

    /** 判断指定任务 key 是否在调度器和 jitter 分组中仍处于活跃状态。 */
    boolean hasActiveTask(String taskKey) {
        if (scheduleRegistrar instanceof TaskAwareScheduleRegistrar aware) {
            return hasPendingJitterGroup(taskKey) && aware.hasTask(new ScheduleKey("canvas", taskKey));
        }
        return hasPendingJitterGroup(taskKey);
    }

    /** 判断传入分组是否仍是当前任务 key 对应的活跃分组。 */
    boolean isCurrentPendingJitterGroup(String taskKey, PendingJitterGroup group) {
        synchronized (lifecycleLock) {
            return pendingJitterTasks.get(taskKey) == group;
        }
    }

    /** 在注册失败或取消时移除指定 jitter 分组，并释放未执行任务。 */
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

    /** 一次性任务触发完成且 jitter 队列清空后，注销调度并清理分组。 */
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

    /** 根据 cron 或单次触发时间构建调度注册对象。 */
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
            triggerTime = parseTriggerTime(triggerTimeStr, timezone);
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

    private LocalDateTime parseTriggerTime(String triggerTimeStr, String timezone) {
        try {
            return LocalDateTime.parse(triggerTimeStr);
        } catch (DateTimeParseException ignored) {
            return OffsetDateTime.parse(triggerTimeStr)
                    .atZoneSameInstant(ZoneId.of(timezone))
                    .toLocalDateTime();
        }
    }

    /** 生成画布定时节点的稳定调度 key。 */
    private ScheduleKey scheduleKey(Long canvasId, String nodeId) {
        return new ScheduleKey("canvas", canvasId + ":" + nodeId);
    }

    /** 按 jitter 延迟安排单个用户触发，取消时通过 Disposable 释放。 */
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
                // 延迟任务无论执行、取消还是失败，都必须从分组中移除以触发空闲清理。
                .doFinally(signalType -> group.remove(pending))
                .subscribe(
                        ignored -> dispatchScheduledTrigger(group, canvasId, userId),
                        e -> log.warn("[SCHEDULER] jitter 调度失败 taskKey={} userId={}: {}", group.taskKey, userId, e.getMessage())
                ));
        return pending;
    }

    /** 将单个用户的定时触发提交到画布执行服务。 */
    void dispatchScheduledTrigger(PendingJitterGroup group, Long canvasId, String userId) {
        if (group.isTerminated()) {
            // 任务已被下线/关闭，延迟到期的 jitter 触发直接丢弃。
            return;
        }
        String nodeId = nodeIdFromTaskKey(group.taskKey);
        backgroundSubscriptions.track(
                "scheduler:trigger:" + group.taskKey + ":" + userId,
                executionService.trigger(
                        canvasId, userId, TriggerType.SCHEDULED,
                        NodeType.SCHEDULED_TRIGGER, nodeId,
                        Map.of(
                                MapFieldKeys.SCHEDULED_BATCH, true,
                                MapFieldKeys.SCHEDULED_TRIGGER_NODE_ID, nodeId
                        ),
                        java.util.UUID.randomUUID().toString(), false),
                e -> log.warn("[SCHEDULER] 用户触发失败 userId={}: {}", userId, e.getMessage()));
    }

    public static boolean isScheduledBatchUser(String userId) {
        return userId != null && userId.startsWith(SCHEDULED_BATCH_USER_PREFIX);
    }

    private static String scheduledBatchUserId(Long canvasId, String nodeId) {
        return SCHEDULED_BATCH_USER_PREFIX + canvasId + ":" + nodeId;
    }

    static String nodeIdFromTaskKey(String taskKey) {
        if (taskKey == null || taskKey.isBlank()) {
            return "";
        }
        int separator = taskKey.indexOf(':');
        return separator >= 0 ? taskKey.substring(separator + 1) : taskKey;
    }

    /** 根据用户来源类型异步解析定时触发目标用户列表。 */
    @SuppressWarnings("unchecked")
    Mono<List<String>> resolveUserIdsAsync(String sourceType, Map<String, Object> src) {
        Map<String, Object> safeSrc = src == null ? Map.of() : src;
        return switch (sourceType) {
            case "USER_LIST" -> Mono.fromCallable(() -> (List<String>) safeSrc.getOrDefault("userIds", List.of()))
                    .subscribeOn(Schedulers.boundedElastic());

            case "TAGGER_GROUP" -> resolveTaggerGroupUserIdsAsync(safeSrc);

            case "USER_API" -> resolveUserApiUserIdsAsync(safeSrc);

            default -> {
                log.warn("[SCHEDULER] 未知 userSource type={}", sourceType);
                yield Mono.just(List.of());
            }
        };
    }

    private Mono<List<String>> resolveTaggerGroupUserIdsAsync(Map<String, Object> src) {
        // 调用 Tagger 离线用户列表接口获取指定标签的用户 ID 列表
        String tagCode = (String) src.get("tagCode");
        int limit = src.get("limit") instanceof Number n ? n.intValue() : 10000;
        int pageSize = src.get("pageSize") instanceof Number n ? n.intValue() : 1000;
        if (tagCode == null) {
            log.warn("[SCHEDULER] TAGGER_GROUP 缺少 tagCode");
            return Mono.just(List.of());
        }
        return fetchTaggerGroupPage(tagCode, Math.max(limit, 0), Math.max(pageSize, 1), 1, new ArrayList<>())
                .doOnNext(result -> log.info("[SCHEDULER] TAGGER_GROUP tagCode={} 拉取 {} 个用户", tagCode, result.size()))
                .onErrorResume(e -> {
                    log.error("[SCHEDULER] TAGGER_GROUP 用户列表拉取失败: {}", e.getMessage());
                    return Mono.just(List.of());
                });
    }

    @SuppressWarnings("unchecked")
    private Mono<List<String>> fetchTaggerGroupPage(
            String tagCode, int limit, int pageSize, int page, List<String> result) {
        if (result.size() >= limit) {
            return Mono.just(trimToLimit(result, limit));
        }
        return taggerClient.get()
                .uri(u -> u.path("/offline/users")
                        .queryParam("tagCode", tagCode)
                        .queryParam("page", page)
                        .queryParam("size", pageSize)
                        .build())
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .flatMap(resp -> {
                    List<String> batch = resp != null
                            ? (List<String>) resp.getOrDefault("userIds", List.of())
                            : List.of();
                    result.addAll(batch);
                    if (batch.size() < pageSize) {
                        return Mono.just(trimToLimit(result, limit));
                    }
                    return fetchTaggerGroupPage(tagCode, limit, pageSize, page + 1, result);
                });
    }

    @SuppressWarnings("unchecked")
    private Mono<List<String>> resolveUserApiUserIdsAsync(Map<String, Object> src) {
        // 调用自定义接口获取用户列表
        String apiKey = (String) src.get("apiKey");
        if (apiKey == null) {
            log.warn("[SCHEDULER] USER_API 缺少 apiKey");
            return Mono.just(List.of());
        }
        return apiCallClient.post()
                .uri("/call")
                .bodyValue(java.util.Map.of(
                        MapFieldKeys.API_KEY, apiKey,
                        MapFieldKeys.PARAMS, src.getOrDefault(MapFieldKeys.PARAMS, java.util.Map.of())))
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .map(resp -> resp != null ? (List<String>) resp.getOrDefault("userIds", List.of()) : List.<String>of())
                .doOnNext(userIds -> log.info("[SCHEDULER] USER_API apiKey={} 返回 {} 个用户", apiKey, userIds.size()))
                .onErrorResume(e -> {
                    log.error("[SCHEDULER] USER_API 用户列表获取失败: {}", e.getMessage());
                    return Mono.just(List.of());
                });
    }

    private static List<String> trimToLimit(List<String> result, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return List.copyOf(result.subList(0, Math.min(result.size(), limit)));
    }

    /** 生成 [0, jitterMaxMs) 区间内的随机延迟；jitterMaxMs=0 时返回 ZERO。可静态调用，便于单测。 */
    static Duration calcJitter(long jitterMaxMs) {
        if (jitterMaxMs <= 0) return Duration.ZERO;
        return Duration.ofMillis(java.util.concurrent.ThreadLocalRandom.current().nextLong(0, jitterMaxMs));
    }

    /** 单个调度任务下的 jitter 延迟任务集合，负责取消、关闭和空闲清理。 */
    static final class PendingJitterGroup {
        /** 抖动任务分组 key，用于日志和清理。 */
        private final String taskKey;
        /** 当前分组尚未完成的响应式任务集合。 */
        private final Set<Disposable> pending = ConcurrentHashMap.newKeySet();
        /** 当前分组待执行任务数量。 */
        private final AtomicInteger size = new AtomicInteger();
        /** 是否在任务清空后关闭分组。 */
        private final AtomicBoolean closeWhenIdle = new AtomicBoolean(false);
        /** 分组是否已终止。 */
        private final AtomicBoolean terminated = new AtomicBoolean(false);
        /** 分组清理是否已触发，防止重复清理。 */
        private final AtomicBoolean cleanupTriggered = new AtomicBoolean(false);
        /** 分组空闲回调，用于从 pendingJitterTasks 中移除自身。 */
        private volatile Runnable onIdle;

        /** 创建与指定调度 taskKey 绑定的 jitter 分组。 */
        private PendingJitterGroup(String taskKey) {
            this.taskKey = taskKey;
        }

        /** 添加一个待执行延迟任务，分组终止或关闭时拒绝添加。 */
        boolean add(Disposable disposable) {
            if (terminated.get() || closeWhenIdle.get()) {
                return false;
            }
            if (!pending.add(disposable)) {
                return false;
            }
            size.incrementAndGet();
            if (terminated.get() || closeWhenIdle.get()) {
                // add 与 terminate/closeWhenIdle 竞态时回滚计数并取消刚加入的任务。
                if (pending.remove(disposable)) {
                    size.decrementAndGet();
                }
                disposable.dispose();
                return false;
            }
            return true;
        }

        /** 移除已完成或已取消的延迟任务，并在空闲时触发清理。 */
        void remove(Disposable disposable) {
            if (pending.remove(disposable)) {
                size.decrementAndGet();
                cleanupIfIdle();
            }
        }

        /** 标记分组在所有延迟任务完成后执行空闲清理回调。 */
        void closeWhenIdle(Runnable onIdle) {
            this.onIdle = onIdle;
            closeWhenIdle.set(true);
            cleanupIfIdle();
        }

        /** 在分组关闭且无待执行任务时只触发一次清理回调。 */
        private void cleanupIfIdle() {
            Runnable cleanup = onIdle;
            if (closeWhenIdle.get() && size.get() == 0 && cleanup != null
                    && cleanupTriggered.compareAndSet(false, true)) {
                // 一次性任务所有 jitter 都结束后，只触发一次注册表清理。
                cleanup.run();
            }
        }

        /** 标记分组终止，阻止后续新增或执行任务。 */
        void terminate() {
            terminated.set(true);
        }

        /** 返回分组是否已终止。 */
        boolean isTerminated() {
            return terminated.get();
        }

        /** 判断当前分组是否允许立即执行零 jitter 任务。 */
        boolean canScheduleImmediate() {
            return !terminated.get() && !closeWhenIdle.get();
        }

        /** 取消并清空所有待执行延迟任务。 */
        void dispose() {
            terminated.set(true);
            for (Disposable disposable : pending) {
                disposable.dispose();
            }
            pending.clear();
            size.set(0);
        }
    }

    /** 可查询任务是否存在的调度注册器扩展接口，供测试和状态判断使用。 */
    private interface TaskAwareScheduleRegistrar {
        /**
         * 判断 has Task 相关的业务数据。
         *
         * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
         *
         * @param key key 对应的缓存键、配置键或业务键
         * @return 判断结果，true 表示校验通过或条件成立
         */
        boolean hasTask(ScheduleKey key);
    }

    /** Spring 任务调度器适配实现，用本地 TaskScheduler 承载 cron 和一次性任务。 */
    private static final class LegacyTaskSchedulerRegistrar implements ScheduleRegistrar, TaskAwareScheduleRegistrar {
        /** Spring TaskScheduler 实例，负责实际注册本机调度任务。 */
        private final org.springframework.scheduling.TaskScheduler taskScheduler;
        /** 已注册的定时任务映射。 */
        private final Map<ScheduleKey, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
        /** 注册过程中被取消的任务 key 集合，用于处理并发取消。 */
        private final Set<ScheduleKey> cancelledDuringRegistration = ConcurrentHashMap.newKeySet();

        /** 注入 Spring TaskScheduler，作为默认本地调度注册器。 */
        private LegacyTaskSchedulerRegistrar(org.springframework.scheduling.TaskScheduler taskScheduler) {
            this.taskScheduler = taskScheduler;
        }

        /** 注册 cron 或一次性调度任务，并处理注册期间并发取消的竞态。 */
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

        /** 注销调度任务。 */
        @Override
        public void unregister(ScheduleKey key) {
            ScheduledFuture<?> future = tasks.remove(key);
            if (future != null) {
                future.cancel(false);
            } else {
                cancelledDuringRegistration.add(key);
            }
        }

        /** 判断指定调度任务是否已注册。 */
        @Override
        public boolean hasTask(ScheduleKey key) {
            return tasks.containsKey(key);
        }
    }
}
