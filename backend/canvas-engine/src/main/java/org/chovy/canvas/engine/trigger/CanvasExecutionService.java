package org.chovy.canvas.engine.trigger;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.cache.CanvasEntityCache;
import org.chovy.canvas.infrastructure.mq.OverflowRetryMessage;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import jakarta.annotation.PostConstruct;
import org.chovy.canvas.perf.PerfRunContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper;

/**
 * 画布执行编排器：dedup → ctx 加载/初始化 → DAG 执行 → 结果写入 DB。
 * 适用于所有触发方式（MQ / 直调 / 行为 / 定时）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CanvasExecutionService {

    /** 画布 Mapper，用于 dry-run 和版本解析场景读取画布主表。 */
    private final CanvasMapper canvasMapper;
    /** 画布版本 Mapper。 */
    private final CanvasVersionMapper canvasVersionMapper;
    /** 画布执行记录 Mapper。 */
    private final CanvasExecutionMapper executionMapper;
    /** 画布配置缓存，用于执行链路快速读取发布态配置。 */
    private final CanvasConfigCache configCache;
    /** DAG 解析器，将 graphJson 转换为可执行图结构。 */
    private final DagParser dagParser;
    /** 执行上下文 Redis 持久化服务。 */
    private final ContextPersistenceService ctxStore;
    /** DAG 执行引擎。 */
    private final DagEngine dagEngine;
    /** 触发预检服务，负责有效期、配额、冷却期和并发保护校验。 */
    private final TriggerPreCheckService preCheckService;
    /** 运行中执行注册表，控制并发和取消。 */
    private final InFlightExecutionRegistry executionRegistry;
    /** 画布执行统计 Mapper。 */
    private final CanvasExecutionStatsMapper statsMapper;
    /** 画布实体缓存。 */
    private final CanvasEntityCache canvasEntityCache;
    /** MQ 触发节点处理器，用于解析和匹配 MQ 触发入口。 */
    private final MqTriggerHandler mqTriggerHandler;
    /** 画布执行死信 Mapper。 */
    private final CanvasExecutionDlqMapper dlqMapper;
    /** 触发优先级配置。 */
    private final TriggerPriorityConfig priorityConfig;
    /** RocketMQ 模板，用于溢出重试和降级消息投递。 */
    private final RocketMQTemplate rocketMQTemplate;
    /** Jackson ObjectMapper，用于 JSON 序列化和反序列化。 */
    private final ObjectMapper objectMapper;
    /** CDP 用户服务，用于导入或执行时创建用户画像。 */
    private final CdpUserService cdpUserService;
    /** Disruptor 派发服务。 */
    private final CanvasDisruptorService disruptorService;
    /** 阻塞式 Redis 模板，用于锁、去重、票据或跨实例通知。 */
    private final StringRedisTemplate redis;
    /** Redis key 工具，集中生成执行相关 key。 */
    private final RedisKeyUtil redisKeys;
    /** 雪花 ID 生成器。 */
    private final Snowflake snowflake;

    /** 执行上下文在 Redis 中的保留秒数。 */
    @Value("${canvas.execution.context-ttl-sec:86400}")
    private long ctxTtlSec;

    /** 单次画布执行全局超时时间。 */
    @Value("${canvas.execution.global-timeout-sec:600}")
    private long globalTimeoutSec;

    /** 全局最大并发执行数。 */
    @org.springframework.beans.factory.annotation.Value("${canvas.execution.max-concurrency:1000}")
    private int globalMaxConcurrency;

    // ── 启动校验 ──────────────────────────────────────────────────

    /**
     * 启动时校验 globalMaxConcurrency 在集群内的一致性。
     *
     * <p>用 Redis SETNX 抢占写入本机配置值：
     * <ul>
     *   <li>首台启动的实例：写入成功，该值成为集群基准；</li>
     *   <li>后续实例：读取已有值，若与本机配置不符则抛出 {@link IllegalStateException}（fail-fast），
     *       避免不同实例用不同的并发上限悄无声息地破坏全局限流语义。</li>
     * </ul>
     *
     * <p>注意：若需变更 globalMaxConcurrency，需先停止所有实例、删除 Redis key，
     * 再以新配置重新部署。
     */
    @PostConstruct
    void validateGlobalMaxConcurrencyConsistency() {
        String key = redisKeys.globalMaxConcurrencyConfig();
        String localVal = String.valueOf(globalMaxConcurrency);
        Boolean isNew = redis.opsForValue().setIfAbsent(key, localVal);
        if (!Boolean.TRUE.equals(isNew)) {
            String stored = redis.opsForValue().get(key);
            if (!localVal.equals(stored)) {
                throw new IllegalStateException(
                        "[STARTUP] canvas.execution.max-concurrency 集群内配置不一致！" +
                        " 本机=" + localVal + "，Redis存储值=" + stored +
                        "。请统一配置后重新部署，或手动删除 Redis key: " + key);
            }
        }
        log.info("[STARTUP] globalMaxConcurrency={} 集群一致性校验通过", globalMaxConcurrency);
    }

    // ── 触发入口 ──────────────────────────────────────────────────

    /**
     * dry-run 专用入口：直接传入 graphJson，跳过 DB draft 读取。
     * 解决"配置未保存到 DB 时，dry-run 用旧数据"的问题。
     */
    public Mono<Map<String, Object>> triggerDryRun(
            Long canvasId, String userId,
            Map<String, Object> payload, String graphJson) {
        return Mono.fromCallable(() -> {
                    CanvasDO canvas = canvasMapper.selectById(canvasId);
                    if (canvas == null) throw new IllegalStateException("画布不存在: " + canvasId);

                    ExecutionContext ctx = newContext(canvasId, -1L, userId, TriggerType.DRY_RUN);
                    ctx.getTriggerPayload().putAll(sanitizePayload(payload));
                    ctx.setPerfRunId(PerfRunContext.extract(ctx.getTriggerPayload()));

                    DagGraph graph;
                    if (graphJson != null && !graphJson.isBlank()) {
                        graph = dagParser.parse(graphJson);
                    } else {
                        Long versionId = resolveVersionId(canvas, userId, true);
                        graph = configCache.get(canvasId, versionId);
                        ctx.setVersionId(versionId);
                    }

                    String triggerNodeId = findTriggerNode(graph, NodeType.DIRECT_CALL, null);
                    if (triggerNodeId == null) {
                        triggerNodeId = graph.entryNodes().stream().findFirst().orElse(null);
                    }
                    if (triggerNodeId == null)
                        throw new IllegalStateException("画布没有入口节点，请确保存在触发器节点");

                    return Map.of(
                            MapFieldKeys.CTX, ctx,
                            MapFieldKeys.GRAPH, graph,
                            MapFieldKeys.TRIGGER_NODE_ID, triggerNodeId);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(prep -> {
                    if (!prep.containsKey(MapFieldKeys.CTX)) {
                        return Mono.just(prep);
                    }
                    ExecutionContext ctx = (ExecutionContext) prep.get(MapFieldKeys.CTX);
                    DagGraph graph = (DagGraph) prep.get(MapFieldKeys.GRAPH);
                    String triggerNodeId = (String) prep.get(MapFieldKeys.TRIGGER_NODE_ID);

                    ensureCdpUser(ctx);
                    CanvasExecutionDO exec = createExecution(ctx);

                    return insertExecution(exec)
                            .then(dagEngine.execute(graph, triggerNodeId, ctx)
                                    .timeout(Duration.ofSeconds(globalTimeoutSec))
                                    .flatMap(result -> {
                                        Map<String, Object> resp = new HashMap<>(result);
                                        resp.put(MapFieldKeys.EXECUTION_ID, ctx.getExecutionId());
                                        return updateExecution(exec, ExecutionStatus.SUCCESS.getCode(), result)
                                                .thenReturn(resp);
                                    })
                                    .onErrorResume(e -> {
                                        log.error("[DRY_RUN] 执行失败: {}", e.getMessage());
                                        Map<String, Object> resp = Map.of(
                                                MapFieldKeys.ERROR, e.getMessage(),
                                                MapFieldKeys.EXECUTION_ID, ctx.getExecutionId()
                                        );
                                        return updateExecution(exec, ExecutionStatus.FAILED.getCode(),
                                                Map.of(MapFieldKeys.ERROR, e.getMessage()))
                                                .thenReturn(resp);
                                    }));
                });
    }

    /**
     * 触发画布执行：执行工作流包含 dedup 检查、上下文准备、DAG 执行以及结果持久化。
     */
    public Mono<Map<String, Object>> trigger(
            Long canvasId, String userId, String triggerType,
            String triggerNodeType, String matchKey,
            Map<String, Object> payload, String msgId, boolean dryRun) {
        return triggerInternal(canvasId, userId, triggerType, triggerNodeType, matchKey,
                payload, msgId, dryRun, false, 0, false, 0, null);
    }

    /**
     * Persistent execution request entrypoint.
     * Overflow is returned to the request table retry policy instead of creating
     * an additional RocketMQ overflow message.
     */
    public Mono<Map<String, Object>> triggerFromExecutionRequest(
            Long canvasId, String userId, String triggerType,
            String triggerNodeType, String matchKey,
            Map<String, Object> payload, String msgId) {
        return triggerFromExecutionRequest(canvasId, userId, triggerType, triggerNodeType,
                matchKey, payload, msgId, 0, null);
    }

    /** 从执行请求队列触发画布执行。 */
    public Mono<Map<String, Object>> triggerFromExecutionRequest(
            Long canvasId, String userId, String triggerType,
            String triggerNodeType, String matchKey,
            Map<String, Object> payload, String msgId,
            int priorAttemptCount, String lastError) {
        return triggerInternal(canvasId, userId, triggerType, triggerNodeType, matchKey,
                payload, msgId, false, false, 0, true, priorAttemptCount, lastError);
    }

    /**
     * Internal Disruptor entrypoint. The dispatch options token is not publicly
     * constructible, so business payloads cannot forge overflow-retry status.
     */
    public Mono<Map<String, Object>> triggerFromDisruptor(
            Long canvasId,
            String userId,
            String triggerType,
            String triggerNodeType,
            String matchKey,
            Map<String, Object> payload,
            String msgId,
            CanvasDisruptorService.DispatchOptions dispatchOptions
    ) {
        boolean overflowRetry = dispatchOptions != null && dispatchOptions.isOverflowRetry();
        int overflowChainRetryCount = dispatchOptions != null
                ? dispatchOptions.getOverflowChainRetryCount()
                : 0;
        return triggerInternal(canvasId, userId, triggerType, triggerNodeType, matchKey,
                payload, msgId, false, overflowRetry, overflowChainRetryCount, false, 0, null);
    }

    /** 统一触发入口，串联准备阶段和执行阶段的响应式流程。 */
    private Mono<Map<String, Object>> triggerInternal(
            Long canvasId, String userId, String triggerType,
            String triggerNodeType, String matchKey,
            Map<String, Object> payload, String msgId, boolean dryRun,
            boolean overflowRetry, int overflowChainRetryCount,
            boolean persistentRequest, int priorAttemptCount, String lastError) {
        return Mono.fromCallable(() ->
                        // 校验与准备
                        prepareExecution(
                                canvasId, userId, triggerType,
                                triggerNodeType, matchKey, payload,
                                msgId, dryRun, overflowRetry,
                                overflowChainRetryCount, persistentRequest,
                                priorAttemptCount, lastError))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(
                        // 执行阶段
                        prep -> executeFromPrep(canvasId, userId, dryRun, prep));
    }

    /**
     * 执行阶段：获取执行槽位、消费配额、执行 DAG、处理结果
     */
    private Mono<? extends Map<String, Object>> executeFromPrep(Long canvasId, String userId, boolean dryRun, Map<String, ?> prep) {
        if (!prep.containsKey(MapFieldKeys.CTX)) {
            return Mono.just(new HashMap<String, Object>(prep));
        }
        // ============== 准备执行画布相关信息 ==============
        // 单次画布执行的上下文
        ExecutionContext ctx = (ExecutionContext) prep.get(MapFieldKeys.CTX);
        // 画布 DAG 图
        DagGraph graph = (DagGraph) prep.get(MapFieldKeys.GRAPH);
        // 触发器节点 ID
        String triggerNodeId = (String) prep.get(MapFieldKeys.TRIGGER_NODE_ID);
        // 是否是恢复执行
        boolean isResume = (Boolean) prep.get(MapFieldKeys.IS_RESUME);
        // 画布实例
        CanvasDO canvas = (CanvasDO) prep.get(MapFieldKeys.CANVAS);
        // 幂等键
        String acquiredDedupKey = (String) prep.get(MapFieldKeys.DEDUP_KEY);
        // 准入上限
        int admissionLimit = (Integer) prep.get(MapFieldKeys.ADMISSION_LIMIT);

        SlotAcquisitionResult slotResult = tryAcquireSlot(
                canvasId, ctx, admissionLimit, dryRun, isResume, acquiredDedupKey);
        if (slotResult.isOverflow()) return slotResult.overflowMono();

        return doExecute(canvasId, userId, dryRun,
                canvas, slotResult.slot(), ctx,
                isResume, acquiredDedupKey,
                graph, triggerNodeId);
    }

    /**
     * 执行阶段：扣减配额 → CDP 用户相关信息落库 → 创建执行记录 → 运行 DAG → 处理结果。
     *
     * <p> 资源释放责任链（异常时）：
     * <ul>
     *   <li>consumeQuotaAndRecord 失败 → 释放 slot + resumeLock + dedupKey，返回 error；</li>
     *   <li>DAG 执行失败（onErrorResume）→ 更新执行状态为 FAILED，
     *       释放 resumeLock + dedupKey（doOnError）；</li>
     *   <li>finally（doFinally）→ 注销 registry slot（无论成功失败）。</li>
     * </ul>
     *
     * <p> PAUSED 状态处理：
     * DAG 执行后若 ctx 中存在 WAITING 状态节点，视为挂起：
     * 保存上下文到 Redis（供下次恢复），释放 resumeLock，更新执行状态为 PAUSED。
     *
     * <p>
     * 执行 DAG 主流程，分三步：
     * <ol>
     *   <li>配额扣减 — 非 dryRun 且非 WAIT/GOAL 恢复触发时扣减，失败立即释放锁并返回错误</li>
     *   <li>准备执行记录 — 初始化 CDP 用户、创建 CanvasExecutionDO、构建 executionMono</li>
     *   <li>响应式链组装 — 持久化记录 → 绑定取消句柄 → 成功/失败回调 → 释放分布式锁</li>
     * </ol>
     */
    private Mono<Map<String, Object>> doExecute(
            Long canvasId, String userId, boolean dryRun,
            CanvasDO canvas, Disposable.Swap finalExecutionSlot,
            ExecutionContext ctx, boolean isResume, String acquiredDedupKey,
            DagGraph graph, String triggerNodeId) {

        // 1. 配额扣减（WAIT/GOAL 恢复触发跳过，防止配额虚耗）
        if (!dryRun) {
            try {
                consumeQuotaIfNeeded(canvas, userId, ctx);
            } catch (RuntimeException e) {
                releaseResources(canvasId, finalExecutionSlot, ctx, isResume, acquiredDedupKey);
                return Mono.error(e);
            }
        }

        // 2. 保存 CDP 用户相关信息
        ensureCdpUser(ctx);
        final CanvasExecutionDO finalExec = buildExecutionRecord(ctx, acquiredDedupKey);

        // 3. 运行 DAG
        Mono<Map<String, Object>> executionMono = dagEngine.execute(graph, triggerNodeId, ctx)
                .timeout(Duration.ofSeconds(globalTimeoutSec));

        // 4. 组装响应式链：持久化记录 → 绑定取消句柄 → 处理结果/错误 → 释放分布式锁
        return insertExecution(finalExec)
                .then(executionMono
                        .doOnSubscribe(sub -> {
                            if (finalExecutionSlot != null) finalExecutionSlot.update(sub::cancel);
                        })
                        .flatMap(result -> handleSuccess(result, finalExec, ctx, graph, dryRun, isResume))
                        .onErrorResume(e -> handleError(e, finalExec, ctx, graph, dryRun, isResume)))
                .doOnError(e -> releaseLocks(ctx, isResume, acquiredDedupKey))
                .doFinally(signal -> {
                    if (!dryRun) executionRegistry.deregister(canvasId, ctx.getExecutionId());
                });
    }

    /** 非 WAIT/GOAL 恢复触发时扣减配额，恢复路径跳过以防虚耗。 */
    private void consumeQuotaIfNeeded(CanvasDO canvas, String userId, ExecutionContext ctx) {
        if (!ctx.isQuotaBypass() && !isInternalContinuationTrigger(ctx.getTriggerType())) {
            preCheckService.consumeQuotaAndRecord(canvas, userId);
        }
    }

    /** 基于执行上下文创建执行记录，并在压测链路保存实际获取的幂等键。 */
    private CanvasExecutionDO buildExecutionRecord(ExecutionContext ctx, String acquiredDedupKey) {
        CanvasExecutionDO exec = createExecution(ctx);
        if (ctx.getPerfRunId() != null && acquiredDedupKey != null) {
            exec.setLastDedupKey(acquiredDedupKey);
        }
        return exec;
    }

    /** 释放 resumeLock 和 dedupKey，用于响应式链 doOnError 回调。 */
    private void releaseLocks(ExecutionContext ctx, boolean isResume, String acquiredDedupKey) {
        if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId(), ctx.getResumeLockToken());
        if (acquiredDedupKey != null) ctxStore.releaseDedup(acquiredDedupKey);
    }

    /** 释放执行槽 + 分布式锁，用于配额扣减失败时的同步清理。 */
    private void releaseResources(Long canvasId, Disposable.Swap finalExecutionSlot,
            ExecutionContext ctx, boolean isResume, String acquiredDedupKey) {
        if (finalExecutionSlot != null) executionRegistry.deregister(canvasId, ctx.getExecutionId());
        releaseLocks(ctx, isResume, acquiredDedupKey);
    }

    /** 处理 DAG 成功返回后的执行状态、上下文快照和统计更新。 */
    private Mono<Map<String, Object>> handleSuccess(
            Map<String, Object> result, CanvasExecutionDO finalExec,
            ExecutionContext ctx, DagGraph graph, boolean dryRun, boolean isResume) {
        boolean paused = isPaused(ctx, graph);
        if (paused) {
            if (!dryRun) {
                ctxStore.save(ctx);
                if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId(), ctx.getResumeLockToken());
                incrementStats(ctx.getCanvasId(), ExecutionStatus.PAUSED.getCode(), ctx.getUserId());
            }
            Map<String, Object> resp = new HashMap<>(result);
            resp.put(MapFieldKeys.EXECUTION_ID, ctx.getExecutionId());
            return updateExecution(finalExec, ExecutionStatus.PAUSED.getCode(), result)
                    .thenReturn(resp);
        }
        if (!dryRun) {
            ctxStore.delete(ctx.getCanvasId(), ctx.getUserId());
            if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId(), ctx.getResumeLockToken());
            incrementStats(ctx.getCanvasId(), ExecutionStatus.SUCCESS.getCode(), ctx.getUserId());
        }
        Map<String, Object> resp = new HashMap<>(result);
        resp.put(MapFieldKeys.EXECUTION_ID, ctx.getExecutionId());
        return updateExecution(finalExec, ExecutionStatus.SUCCESS.getCode(), result)
                .thenReturn(resp);
    }

    /** 处理 DAG 执行异常，区分暂停状态和失败状态并持久化结果。 */
    private Mono<Map<String, Object>> handleError(
            Throwable e, CanvasExecutionDO finalExec,
            ExecutionContext ctx, DagGraph graph, boolean dryRun, boolean isResume) {
        log.error("[ENGINE] 执行失败 executionId={}: {}", ctx.getExecutionId(), e.getMessage());
        boolean paused = isPaused(ctx, graph);
        Mono<Void> updateMono;
        if (paused) {
            if (!dryRun) {
                ctxStore.save(ctx);
                if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId(), ctx.getResumeLockToken());
            }
            updateMono = updateExecution(finalExec, ExecutionStatus.PAUSED.getCode(), Map.of());
            if (!dryRun)
                incrementStats(ctx.getCanvasId(), ExecutionStatus.PAUSED.getCode(), ctx.getUserId());
        } else {
            if (!dryRun) {
                ctxStore.delete(ctx.getCanvasId(), ctx.getUserId());
                if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId(), ctx.getResumeLockToken());
            }
            updateMono = updateExecution(finalExec, ExecutionStatus.FAILED.getCode(),
                    Map.of(MapFieldKeys.ERROR, e.getMessage()));
            if (!dryRun)
                incrementStats(ctx.getCanvasId(), ExecutionStatus.FAILED.getCode(), ctx.getUserId());
        }
        Map<String, Object> resp = Map.of(
                MapFieldKeys.ERROR, e.getMessage(),
                MapFieldKeys.EXECUTION_ID, ctx.getExecutionId()
        );
        return updateMono.thenReturn(resp);
    }

    /**
     * 准备阶段：校验画布、去重、前置检查、并发配额、创建执行上下文。
     *
     * <p>返回值约定：
     * <ul>
     *   <li>含 {@code CTX} key → 正常流程，继续执行 DAG；</li>
     *   <li>含 {@code DEDUPLICATED=true} → 幂等拦截，不执行；</li>
     *   <li>含 {@code SKIPPED} → resume-lock 竞争失败，放弃本次；</li>
     *   <li>含 {@code OVERFLOW} → 并发超限，已入队重试或已丢弃。</li>
     * </ul>
     *
     * <p>分布式行为说明：
     * <ul>
     *   <li>步骤A：{@code canvasEntityCache}——Caffeine L1（JVM 本地），画布状态变更有缓存延迟；</li>
     *   <li>步骤B：dedup——Redis SETNX，跨机原子，安全；</li>
     *   <li>步骤C：{@code checkWithoutQuotaAccounting} 冷却期读 DB，有异步延迟，见该方法注释；</li>
     *   <li>步骤D：{@code resolveAdmission} 用 {@link InFlightExecutionRegistry}——Redis ZSET，
     *       跨机原子，已修复分布式并发控制问题；</li>
     *   <li>步骤E：resumeLock——Redis SETNX，跨机原子，安全。</li>
     * </ul>
     */
    private Map<String, ?> prepareExecution(Long canvasId, String userId, String triggerType, String triggerNodeType, String matchKey, Map<String, Object> payload, String msgId, boolean dryRun, boolean overflowRetry, int overflowChainRetryCount, boolean persistentRequest, int priorAttemptCount, String lastError) {

        // A. 从 Caffeine L1 缓存加载画布实体，验证存在且已发布
        //    ⚠️ 分布式：L1 是 JVM 本地，画布下线后最多延迟一个缓存 TTL 才感知
        CanvasDO canvas = validateAndLoadCanvas(canvasId, dryRun);
        boolean persistentRetry = persistentRequest && priorAttemptCount > 0;
        boolean persistentRetryAfterOverflow = persistentRetry && "request_retry".equals(lastError);
        boolean internalContinuation = isInternalContinuationTrigger(triggerType);
        boolean quotaBypass = internalContinuation || (persistentRetry && !persistentRetryAfterOverflow);

        // B. 幂等去重（Redis SETNX，跨机安全）
        //    isResume=true 表示 Redis 中存有该用户的上下文快照（画布已暂停等待中）
        boolean isResume = !dryRun && ctxStore.exists(canvasId, userId);
        DedupResult dedup = performDedupCheck(canvasId, userId, msgId, isResume, dryRun, overflowRetry,
                persistentRequest, internalContinuation);
        if (dedup.deduplicated()){
            return Map.of(MapFieldKeys.DEDUPLICATED, true);
        }
        String acquiredDedupKey = dedup.dedupKey();

        // C. 前置资格校验（不扣减配额）
        //    WAIT/GOAL 恢复触发跳过此检查：恢复不是新触发，冷却期不应拦截恢复路径
        if (!dryRun && !isInternalContinuationTrigger(triggerType) && !persistentRetry) {
            preCheckService.checkWithoutQuotaAccounting(canvas, userId);
            if (overflowRetry) {
                log.debug("[ENGINE] 溢出重试跳过配额扣减（扣减在 doExecute 中进行）canvasId={} userId={}", canvasId, userId);
            }
        }

        // D. 并发配额检查（JVM 本地计数，不跨机）
        //    多机部署时实际并发上限 = effectiveMax × 机器数
        int admissionLimit = globalMaxConcurrency;
        if (!dryRun) {
            AdmissionResult admission = resolveAdmission(
                    canvasId, userId, triggerType, triggerNodeType,
                    matchKey, payload, msgId, canvas,
                    overflowChainRetryCount, persistentRequest);
            if (admission.isOverflow()) return admission.overflowResponse();
            admissionLimit = admission.admissionLimit();
        }

        // E. 创建/恢复执行上下文，resumeLock 跨机安全（Redis SETNX）
        return buildPrepMap(
                canvasId, userId, triggerType,
                triggerNodeType, matchKey, payload,
                dryRun, isResume, canvas,
                admissionLimit, acquiredDedupKey, quotaBypass);
    }

    /**
     * 创建或恢复执行上下文，并构建 prep Map。
     *
     * <p>两条路径：
     * <ul>
     *   <li>isResume=true（Redis 中有上下文快照）：
     *       先抢 resumeLock（Redis SETNX，防止多个触发并发恢复同一份上下文），
     *       再从 Redis 反序列化上下文；若 Redis 快照丢失则创建新上下文；</li>
     *   <li>isResume=false：直接创建新的 {@link ExecutionContext}。</li>
     * </ul>
     *
     * <p>resumeLock 分布式语义（跨机安全）：
     * 同一时刻只有一个机器实例能持有 (canvasId, userId) 的 resumeLock，
     * 其他机器抢锁失败时直接放弃本次触发（SKIPPED）。
     * 锁的 TTL = globalTimeoutSec，保证执行超时后锁自动释放。
     */
    private Map<String, Object> buildPrepMap(Long canvasId, String userId, String triggerType, String triggerNodeType, String matchKey, Map<String, Object> payload, boolean dryRun, boolean isResume, CanvasDO canvas, int admissionLimit, String acquiredDedupKey, boolean quotaBypass) {
        boolean resumeLockAcquired = false;
        // 提升到 try 外层，使 catch 块可以用于原子释放锁
        String resumeLockInstanceId = null;
        ExecutionContext ctx;

        try {

            if (isResume && !dryRun) {
                resumeLockInstanceId = UUID.randomUUID().toString();
                if (!ctxStore.acquireResumeLock(canvasId, userId, resumeLockInstanceId, globalTimeoutSec)) {
                    log.warn("[ENGINE] resume-lock 竞争失败，放弃本次触发 canvasId={}", canvasId);
                    return Map.<String, Object>of(MapFieldKeys.SKIPPED, "resume-lock");
                }
                resumeLockAcquired = true;
                ctx = ctxStore.load(canvasId, userId);
                if (ctx == null) {
                    ctx = newContext(canvasId, resolveVersionId(canvas, userId, false), userId, triggerType);
                }
                // 存入 ctx，后续 doExecute / handleSuccess / handleError 用于原子释放锁
                ctx.setResumeLockToken(resumeLockInstanceId);
            } else {
                ctx = newContext(canvasId, resolveVersionId(canvas, userId, dryRun), userId, triggerType);
            }


            // 补充上下文信息
            populateContext(triggerType, triggerNodeType, matchKey, payload, ctx);
            ctx.setQuotaBypass(quotaBypass);


            // 查找画布内容
            DagGraph graph = configCache.get(canvasId, ctx.getVersionId());

            // 查找触发器节点
            String triggerNodeId = findTriggerNode(graph, triggerNodeType, matchKey);
            if (triggerNodeId == null) {
                throw new IllegalStateException(
                        "找不到触发器节点 type=" + triggerNodeType + " key=" + matchKey);
            }

            // 构建返回结果
            return buildPrepareResultMap(
                    isResume, canvas,
                    admissionLimit, acquiredDedupKey,
                    ctx, graph, triggerNodeId
            );
        } catch (RuntimeException e) {
            if (resumeLockAcquired) {
                // 使用 instanceId 原子释放（Lua check-then-del，防误删他机的锁）
                ctxStore.releaseResumeLock(canvasId, userId, resumeLockInstanceId);
            }
            if (acquiredDedupKey != null) {
                ctxStore.releaseDedup(acquiredDedupKey);
            }
            throw e;
        }
    }

    /** 构建准备阶段输出 Map，供执行阶段解包上下文、图和准入信息。 */
    private static Map<String, Object> buildPrepareResultMap(boolean isResume, CanvasDO canvas, int admissionLimit, String acquiredDedupKey, ExecutionContext ctx, DagGraph graph, String triggerNodeId) {
        Map<String, Object> prep = new HashMap<>();
        prep.put(MapFieldKeys.CTX, ctx);
        prep.put(MapFieldKeys.GRAPH, graph);
        prep.put(MapFieldKeys.TRIGGER_NODE_ID, triggerNodeId);
        prep.put(MapFieldKeys.IS_RESUME, isResume);
        prep.put(MapFieldKeys.CANVAS, canvas);
        prep.put(MapFieldKeys.ADMISSION_LIMIT, admissionLimit);
        if (acquiredDedupKey != null) prep.put(MapFieldKeys.DEDUP_KEY, acquiredDedupKey);
        return prep;
    }

    /**
     * 填充执行上下文触发信息
     */
    private void populateContext(String triggerType, String triggerNodeType, String matchKey, Map<String, Object> payload, ExecutionContext ctx) {
        ctx.setTriggerType(triggerType);
        ctx.setTriggerNodeType(triggerNodeType);
        ctx.setMatchKey(matchKey);
        ctx.getTriggerPayload().putAll(sanitizePayload(payload));
        ctx.setPerfRunId(PerfRunContext.extract(ctx.getTriggerPayload()));
    }

    /**
     * 校验画布存在且已发布
     */
    private CanvasDO validateAndLoadCanvas(Long canvasId, boolean dryRun) {
        CanvasDO canvas = canvasEntityCache.get(canvasId);
        if (canvas == null) {
            throw new IllegalStateException("画布不存在: " + canvasId);
        }
        if (!dryRun && !Objects.equals(canvas.getStatus(), CanvasStatusEnum.PUBLISHED.getCode())) {
            throw new IllegalStateException("画布未发布，请先发布后再触发: " + canvasId);
        }
        return canvas;
    }

    // ── 方法提取辅助记录 ──────────────────────────────────────────

    /** 幂等检查结果，标识是否被拦截以及本次持有的 Redis 幂等 key。 */
    private record DedupResult(boolean deduplicated, String dedupKey) {
        static DedupResult duplicate() {
            return new DedupResult(true, null);
        }

        static DedupResult acquired(String key) {
            return new DedupResult(false, key);
        }

        static DedupResult skipped() {
            return new DedupResult(false, null);
        }
    }

    /** 并发准入结果，正常时返回准入上限，超限时返回溢出响应。 */
    private record AdmissionResult(int admissionLimit, Map<String, Object> overflowResponse) {
        static AdmissionResult granted(int limit) {
            return new AdmissionResult(limit, null);
        }

        static AdmissionResult overflow(String code) {
            return new AdmissionResult(0, Map.of(MapFieldKeys.OVERFLOW, code));
        }

        boolean isOverflow() {
            return overflowResponse != null;
        }
    }

    /** 执行槽位获取结果，包含取消句柄或并发超限响应。 */
    private record SlotAcquisitionResult(Disposable.Swap slot, Mono<Map<String, Object>> overflowMono) {
        static SlotAcquisitionResult acquired(Disposable.Swap slot) {
            return new SlotAcquisitionResult(slot, null);
        }

        static SlotAcquisitionResult overflow(Mono<Map<String, Object>> mono) {
            return new SlotAcquisitionResult(null, mono);
        }

        static SlotAcquisitionResult skipped() {
            return new SlotAcquisitionResult(null, null);
        }

        boolean isOverflow() {
            return overflowMono != null;
        }
    }

    /**
     * 幂等去重检查（Redis SETNX，跨机安全）。
     *
     * <p>跳过去重的条件（返回 skipped）：
     * <ul>
     *   <li>dryRun=true；</li>
     *   <li>overflowRetry=true（溢出重试不参与去重，否则重试 msg 永远被首次触发拦截）；</li>
     *   <li>msgId=null（无幂等 key，如 WAIT 恢复等内部触发）。</li>
     * </ul>
     *
     * <p>TTL 设计：
     * isResume 时 TTL 更长（超时 + 600s 容忍），防止上下文恢复过程中 dedupKey 过期导致重复消费。
     *
     * <p>FIXME：isResume 时 dedupTtl = globalTimeoutSec + 600，
     * 但 globalTimeoutSec 可能在运行期被修改（通过配置刷新），导致部分已在途的 dedup 与新值不匹配。
     */
    private DedupResult performDedupCheck(Long canvasId, String userId, String msgId,
                                          boolean isResume, boolean dryRun, boolean overflowRetry,
                                          boolean persistentRequest, boolean internalContinuation) {
        if (dryRun || overflowRetry || persistentRequest || internalContinuation || msgId == null) {
            return DedupResult.skipped();
        }
        // FIXME: 过期时间会发生变化, 判断是否需要调整
        Duration dedupTtl = isResume
                ? Duration.ofSeconds(globalTimeoutSec + 600)
                : Duration.ofHours(24);
        if (!ctxStore.acquireDedup(canvasId, userId, msgId, dedupTtl)) {
            log.debug("[ENGINE] dedup 拦截 canvasId={} userId={} msgId={}", canvasId, userId, msgId);
            return DedupResult.duplicate();
        }
        return DedupResult.acquired(ctxStore.buildDedupKey(canvasId, userId, msgId));
    }

    /**
     * 并发准入决策：按优先级分级处理超限行为。
     *
     * <p>计数来源 {@link InFlightExecutionRegistry}（JVM 本地），多机部署时不跨机同步。
     *
     * <p>三级优先级处理策略：
     * <ul>
     *   <li>HIGH：超 highMax（= maxConc × highRatio）仅打 ERROR 告警，不拒绝，
     *       返回 globalMaxConcurrency 作为 admissionLimit（后续 slot 阶段再卡）；</li>
     *   <li>NORMAL：超 effectiveMax 时发送至 RocketMQ 延迟重试队列，
     *       重试次数通过 {@code overflowChainRetryCount} 累积防止无限重试；</li>
     *   <li>LOW：超 effectiveMax（= maxConc × lowRatio）直接丢弃。</li>
     * </ul>
     */
    private AdmissionResult resolveAdmission(
            Long canvasId, String userId, String triggerType,
            String triggerNodeType, String matchKey,
            Map<String, Object> payload, String msgId,
            CanvasDO canvas, int overflowChainRetryCount, boolean persistentRequest) {
        TriggerPriorityConfig.Priority priority = priorityConfig.of(triggerType);
        // ⚠️ 分布式注意：activeCount 仅统计本 JVM 内的活跃执行
        int active = executionRegistry.activeCount(canvasId);
        // Fix 5: maxTotalExecutions 仅用于总触发量配额，不再作为并发上限
        // 并发上限统一由 canvas.execution.max-concurrency（globalMaxConcurrency）控制
        int maxConc = globalMaxConcurrency;

        // HIGH 优先级：超限仅告警，不拒绝（不能让高优先级任务被低优先级挤死）
        if (priority == TriggerPriorityConfig.Priority.HIGH) {
            int highMax = Math.max(1, (int) (maxConc * priorityConfig.getHighMaxConcurrencyRatio()));
            if (active >= highMax) {
                log.error("[ENGINE] HIGH优先级超并发告警 canvasId={} active={}/{}",
                        canvasId, active, highMax);
            }
            // 返回全局上限，后续 tryAcquireSlot 仍会做最终卡口
            return AdmissionResult.granted(globalMaxConcurrency);
        }

        // LOW 优先级生效上限（= maxConc × lowRatio），NORMAL 使用 maxConc
        int effectiveMax = priority == TriggerPriorityConfig.Priority.LOW
                ? Math.max(1, (int) (maxConc * priorityConfig.getLowRatio()))
                : maxConc;

        if (active >= effectiveMax) {
            log.warn("[ENGINE] 并发上限 canvasId={} active={}/{} priority={}",
                    canvasId, active, effectiveMax, priority);
            if (priority == TriggerPriorityConfig.Priority.NORMAL) {
                if (persistentRequest) {
                    // 来自持久请求表的触发：交由请求表重试策略处理，不再写 MQ
                    return AdmissionResult.overflow("request_retry");
                }
                // Fix 6: 溢出重试次数达到上限，写 DLQ 放弃
                if (overflowChainRetryCount >= priorityConfig.getOverflowMaxRetry()) {
                    log.error("[ENGINE] 溢出重试次数达上限 canvasId={} userId={} retryCount={}/{}，写 DLQ",
                            canvasId, userId, overflowChainRetryCount, priorityConfig.getOverflowMaxRetry());
                    String effectiveMsgId = nonBlank(msgId) ? msgId : "overflow-max-" + UUID.randomUUID();
                    writeOverflowEnqueueDlq(
                            new org.chovy.canvas.infrastructure.mq.OverflowRetryMessage(
                                    canvasId, userId, triggerType, triggerNodeType,
                                    matchKey, sanitizePayload(payload), effectiveMsgId,
                                    overflowChainRetryCount),
                            "max_retry_exceeded:" + overflowChainRetryCount);
                    return AdmissionResult.overflow("max_retry_exceeded");
                }
                // 写入 RocketMQ 延迟队列，稍后重试（携带 chainRetryCount 防循环）
                boolean queued = sendOverflowRetry(canvasId, userId, triggerType, triggerNodeType,
                        matchKey, payload, msgId, overflowChainRetryCount);
                return AdmissionResult.overflow(queued ? "queued_for_retry" : "retry_enqueue_failed");
            }
            // LOW 优先级直接丢弃
            return AdmissionResult.overflow("dropped_low_priority");
        }

        return AdmissionResult.granted(effectiveMax);
    }

    /**
     * 事件触发的 Disruptor 发布，RingBuffer 满时降级为 RocketMQ 溢出重试队列（Fix 2）。
     *
     * <p>补齐不对称：MQ 触发溢出有 RocketMQ 兜底，Event 触发之前直接丢弃。
     * 现统一：先入 RocketMQ 溢出队列，超最大重试次数后写 DLQ。
     */
    public void publishEventWithOverflowFallback(Long canvasId, String userId,
            String eventCode, Map<String, Object> payload, String msgId) {
        try {
            disruptorService.publish(canvasId, userId, TriggerType.EVENT,
                    NodeType.EVENT_TRIGGER, eventCode, payload, msgId);
        } catch (IllegalStateException e) {
            log.warn("[EVENT] Disruptor 溢出，降级 MQ 重试 canvasId={} eventCode={}: {}",
                    canvasId, eventCode, e.getMessage());
            boolean queued = sendOverflowRetry(canvasId, userId, TriggerType.EVENT,
                    NodeType.EVENT_TRIGGER, eventCode, payload, msgId, 0);
            if (!queued) {
                log.error("[EVENT] 溢出事件入队失败，触发丢失 canvasId={} eventCode={}", canvasId, eventCode);
            }
        }
    }

    /**
     * slot 阶段的最终并发卡口（双重保险，resolveAdmission 之后再卡一次）。
     *
     * <p>resolveAdmission 用本地 activeCount 决策，tryAcquireSlot 用 registry.compute 原子写入——
     * 两者合力：前者在轻量路径快速判断，后者保证同 JVM 内原子性。
     *
     * <p>获取失败时释放 resumeLock 和 dedupKey，避免资源泄漏。
     *
     * <p>⚠️ 分布式注意：slot 也是 JVM 本地，与 resolveAdmission 共享"不跨机"的限制。
     */
    private SlotAcquisitionResult tryAcquireSlot(Long canvasId, ExecutionContext ctx,
                                                 int admissionLimit, boolean dryRun,
                                                 boolean isResume, String acquiredDedupKey) {
        if (dryRun) return SlotAcquisitionResult.skipped();
        var acquired = executionRegistry.tryAcquire(
                canvasId, ctx.getExecutionId(), admissionLimit, globalMaxConcurrency);
        if (acquired.isEmpty()) {
            int active = executionRegistry.activeCount(canvasId);
            log.warn("[ENGINE] 画布并发上限已达 canvasId={} active={}/{} global={}/{}",
                    canvasId, active, admissionLimit,
                    executionRegistry.totalActiveCount(), globalMaxConcurrency);
            // 清理已持有的分布式锁和幂等 key，防泄漏
            if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId(), ctx.getResumeLockToken());
            if (acquiredDedupKey != null) ctxStore.releaseDedup(acquiredDedupKey);
            return SlotAcquisitionResult.overflow(
                    Mono.just(Map.of(MapFieldKeys.OVERFLOW, "concurrency_limit_reached",
                            MapFieldKeys.ACTIVE, active, MapFieldKeys.LIMIT, admissionLimit)));
        }
        return SlotAcquisitionResult.acquired(acquired.get());
    }

    // ── 缓存失效 ──────────────────────────────────────────────────

    /**
     * 画布发布/下线时主动驱逐 CanvasDO 实体缓存，
     * 确保下次 trigger() 读到最新状态（已发布/已下线）。
     */
    public void invalidateCanvas(Long canvasId) {
        canvasEntityCache.invalidate(canvasId);
    }

    // ── 私有帮助方法 ──────────────────────────────────────────────

    /** 创建新的执行上下文并写入基础画布、版本、用户和触发信息。 */
    private ExecutionContext newContext(Long canvasId, Long versionId, String userId, String triggerType) {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId(UUID.randomUUID().toString());
        ctx.setCanvasId(canvasId);
        ctx.setVersionId(versionId);
        ctx.setUserId(userId);
        ctx.setTriggerType(triggerType);
        if (userId != null) ctx.getTriggerPayload().put(MapFieldKeys.USER_ID, userId);
        return ctx;
    }

    /** 确保执行上下文中的用户已写入 CDP 用户画像。 */
    private void ensureCdpUser(ExecutionContext ctx) {
        if (StrUtil.isNotBlank(ctx.getUserId())) {
            cdpUserService.ensureUser(ctx.getUserId(), "CANVAS_EXECUTION", ctx.getExecutionId());
        }
    }

    /**
     * 灰度版本解析：根据 userId hash 决定使用灰度版本还是正式版本。
     *
     * <p>dryRun 时优先返回最新草稿版本（draft），无草稿则取最新版本，用于实时预览。
     * 正式执行时：若画布开启灰度（canaryVersionId + canaryPercent > 0），
     * 则对 "{userId}:{canvasId}" 做 hashCode 取模，命中则用灰度版本。
     *
     * <p>注意：hashCode 取模不保证均匀分布（Java String.hashCode 非密码学强散列），
     * 但对于运营画布的灰度场景，均匀性要求不高，可接受。
     */
    private Long resolveVersionId(CanvasDO canvas, String userId, boolean dryRun) {
        if (dryRun) {
            CanvasVersionDO draft = canvasVersionMapper.selectOne(
                    new LambdaQueryWrapper<CanvasVersionDO>()
                            .eq(CanvasVersionDO::getCanvasId, canvas.getId())
                            .eq(CanvasVersionDO::getStatus, 0)
                            .orderByDesc(CanvasVersionDO::getId)
                            .last("LIMIT 1")
            );
            if (draft != null) return draft.getId();
            CanvasVersionDO latest = canvasVersionMapper.selectOne(
                    new LambdaQueryWrapper<CanvasVersionDO>()
                            .eq(CanvasVersionDO::getCanvasId, canvas.getId())
                            .orderByDesc(CanvasVersionDO::getId)
                            .last("LIMIT 1")
            );
            if (latest != null) return latest.getId();
        }
        if (canvas.getCanaryVersionId() != null && canvas.getCanaryPercent() != null
                && canvas.getCanaryPercent() > 0) {
            int bucket = Math.abs((userId + ":" + canvas.getId()).hashCode()) % 100;
            if (bucket < canvas.getCanaryPercent()) {
                log.debug("[CANARY] 命中灰度 canvasId={} userId={} bucket={}/{}",
                        canvas.getId(), userId, bucket, canvas.getCanaryPercent());
                return canvas.getCanaryVersionId();
            }
        }
        return canvas.getPublishedVersionId();
    }

    /**
     * 在 DAG 图中查找与 (triggerNodeType, matchKey) 匹配的触发器节点 ID。
     *
     * <p>匹配规则（按 triggerNodeType 分支）：
     * <ul>
     *   <li>WAIT / GOAL_CHECK：nodeId 直接等于 matchKey（matchKey 即 nodeId）；</li>
     *   <li>MQ_TRIGGER：用 MqTriggerHandler 解析节点 config 中的 topic，与 matchKey 比对；</li>
     *   <li>其他（EVENT_TRIGGER 等）：从 bizConfig 或 config 中取 eventCode / topicKey 与 matchKey 比对。</li>
     * </ul>
     *
     * <p>遍历所有节点（allNodeIds），无法通过 index 加速，但 DAG 节点数通常 < 100，可接受。
     */
    private String findTriggerNode(DagGraph graph, String triggerNodeType, String matchKey) {
        log.debug("[FIND_TRIGGER] triggerNodeType={} matchKey={}", triggerNodeType, matchKey);

        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || !triggerNodeType.equals(node.getType())) continue;
            if (matchesByNodeId(triggerNodeType)) {
                if (matchKey == null || nodeId.equals(matchKey)) return nodeId;
                continue;
            }
            if (matchKey == null) return nodeId;
            Map<String, Object> cfg = new HashMap<>();
            if (node.getBizConfig() != null) cfg.putAll(node.getBizConfig());
            if (node.getConfig() != null) cfg.putAll(node.getConfig());
            String cfgKey = switch (triggerNodeType) {
                case NodeType.MQ_TRIGGER -> mqTriggerHandler.resolveTopic(cfg);
                default -> (String) cfg.getOrDefault("eventCode", cfg.getOrDefault("topicKey", ""));
            };
            if (matchKey.equals(cfgKey)) return nodeId;
        }
        return null;
    }

    /** 判断触发类型是否应直接按节点 ID 与 matchKey 匹配。 */
    private boolean matchesByNodeId(String triggerNodeType) {
        return NodeType.WAIT.equals(triggerNodeType)
                || NodeType.GOAL_CHECK.equals(triggerNodeType)
                || NodeType.HUB.equals(triggerNodeType)
                || NodeType.AGGREGATE.equals(triggerNodeType)
                || NodeType.LOGIC_RELATION.equals(triggerNodeType)
                || NodeType.THRESHOLD.equals(triggerNodeType)
                || NodeType.MANUAL_APPROVAL.equals(triggerNodeType);
    }

    /** 判断执行上下文中是否存在 WAITING 节点状态。 */
    private boolean isPaused(ExecutionContext ctx, DagGraph graph) {
        return ctx.getNodeStatuses().values().stream()
                .anyMatch(s -> s == org.chovy.canvas.engine.context.NodeStatus.WAITING);
    }

    /**
     * 判断是否为 WAIT/GOAL 节点恢复触发（Fix 2）。
     * 这类触发是对已暂停执行的"继续"，不是新触发，不应扣减冷却期和配额。
     */
    private static boolean isInternalContinuationTrigger(String triggerType) {
        return org.chovy.canvas.common.enums.TriggerType.WAIT_RESUME.equals(triggerType)
                || org.chovy.canvas.common.enums.TriggerType.WAIT_TIMEOUT.equals(triggerType)
                || org.chovy.canvas.common.enums.TriggerType.GOAL_CHECK_RESUME.equals(triggerType)
                || org.chovy.canvas.common.enums.TriggerType.GOAL_CHECK_TIMEOUT.equals(triggerType)
                || org.chovy.canvas.common.enums.TriggerType.HUB_TIMEOUT.equals(triggerType)
                || org.chovy.canvas.common.enums.TriggerType.LOGIC_RELATION_TIMEOUT.equals(triggerType)
                || org.chovy.canvas.common.enums.TriggerType.AGGREGATE_TIMEOUT.equals(triggerType)
                || org.chovy.canvas.common.enums.TriggerType.THRESHOLD_TIMEOUT.equals(triggerType)
                || org.chovy.canvas.common.enums.TriggerType.MANUAL_APPROVAL_TIMEOUT.equals(triggerType)
                || "MANUAL_APPROVAL_RESUME".equals(triggerType);
    }

    /** 将并发溢出的触发请求发送到 RocketMQ 延迟重试队列。 */
    private boolean sendOverflowRetry(Long canvasId, String userId, String triggerType,
                                      String triggerNodeType, String matchKey,
                                      Map<String, Object> payload, String msgId, int chainRetryCount) {
        Map<String, Object> retryPayload = sanitizePayload(payload);
        try {
            String effectiveMsgId = nonBlank(msgId)
                    ? msgId
                    : "overflow-" + snowflake.nextIdStr();
            OverflowRetryMessage msg = new OverflowRetryMessage(
                    canvasId, userId,
                    triggerType, triggerNodeType,
                    matchKey, retryPayload,
                    effectiveMsgId, chainRetryCount + 1);
            String tag = triggerType != null ? triggerType : TriggerPriorityConfig.Priority.NORMAL.name();
            Message rocketMsg = new Message(
                    "CANVAS_TRIGGER_OVERFLOW",
                    tag,
                    objectMapper.writeValueAsBytes(msg)
            );
            rocketMsg.setDeliverTimeMs(System.currentTimeMillis() + priorityConfig.getOverflowRetryDelayMs());
            SendResult sendResult = rocketMQTemplate.getProducer().send(rocketMsg);
            if (sendResult == null || sendResult.getSendStatus() != SendStatus.SEND_OK) {
                log.error("[ENGINE] 溢出消息发送未确认成功 canvasId={} status={}",
                        canvasId, sendResult == null ? null : sendResult.getSendStatus());
                writeOverflowEnqueueDlq(msg, "overflow_retry_enqueue_failed:"
                        + (sendResult == null ? "null" : sendResult.getSendStatus()));
                return false;
            }
            log.info("[ENGINE] 溢出事件入队重试 canvasId={} userId={} retryCount={}",
                    canvasId, userId, chainRetryCount + 1);
            return true;
        } catch (Exception e) {
            log.error("[ENGINE] 溢出消息发送失败，事件丢失 canvasId={}: {}", canvasId, e.getMessage());
            OverflowRetryMessage msg = new OverflowRetryMessage(
                    canvasId, userId, triggerType, triggerNodeType,
                    matchKey, retryPayload, nonBlank(msgId) ? msgId : "overflow-" + UUID.randomUUID(),
                    chainRetryCount + 1);
            writeOverflowEnqueueDlq(msg, "overflow_retry_enqueue_failed:" + e.getMessage());
            return false;
        }
    }

    /** 清理内部重试控制字段，避免业务 payload 伪造溢出重试状态。 */
    private Map<String, Object> sanitizePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new HashMap<>(payload);
        sanitized.remove(OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY);
        return sanitized;
    }

    /** 在溢出重试入队失败或达到上限时写入执行死信记录。 */
    private void writeOverflowEnqueueDlq(OverflowRetryMessage msg, String errorMsg) {
        try {
            CanvasExecutionDlqDO dlq = CanvasExecutionDlqDO.builder()
                    .executionId(msg.getMsgId())
                    .canvasId(msg.getCanvasId())
                    .userId(msg.getUserId())
                    .perfRunId(PerfRunContext.extract(msg.getPayload()))
                    .failedNodeId("OVERFLOW_ENQUEUE")
                    .failedNodeType(msg.getTriggerNodeType())
                    .errorMsg(truncate(errorMsg, 500))
                    .retryCount(msg.getChainRetryCount())
                    .triggerPayload(objectMapper.writeValueAsString(msg.getPayload()))
                    .triggerType(msg.getTriggerType())
                    .triggerNodeType(msg.getTriggerNodeType())
                    .matchKey(msg.getMatchKey())
                    .failedAt(LocalDateTime.now())
                    .build();
            dlqMapper.insert(dlq);
        } catch (Exception e) {
            log.error("[ENGINE] 溢出入队失败写DLQ失败 canvasId={} msgId={}: {}",
                    msg.getCanvasId(), msg.getMsgId(), e.getMessage());
        }
    }

    /** 判断字符串是否包含非空白字符。 */
    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    /** 截断字符串到指定最大长度，null 保持不变。 */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.substring(0, Math.min(value.length(), maxLength));
    }

    /** 根据执行上下文创建 RUNNING 状态的画布执行记录。 */
    private CanvasExecutionDO createExecution(ExecutionContext ctx) {
        CanvasExecutionDO exec = new CanvasExecutionDO();
        exec.setId(ctx.getExecutionId());
        exec.setCanvasId(ctx.getCanvasId());
        exec.setVersionId(ctx.getVersionId());
        exec.setUserId(ctx.getUserId());
        exec.setPerfRunId(ctx.getPerfRunId());
        exec.setTriggerType(ctx.getTriggerType());
        exec.setStatus(ExecutionStatus.RUNNING.getCode());
        return exec;
    }

    /** 在线程池中插入执行记录，避免阻塞响应式执行链。 */
    private Mono<Void> insertExecution(CanvasExecutionDO exec) {
        return Mono.fromRunnable(() -> executionMapper.insert(exec))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /** 更新执行状态和结果 JSON，序列化失败时保留状态更新。 */
    private Mono<Void> updateExecution(CanvasExecutionDO exec, int status, Map<String, Object> result) {
        if (exec == null) return Mono.empty();
        exec.setStatus(status);
        try {
            exec.setResult(objectMapper.writeValueAsString(result));
        } catch (Exception ignored) {
        }
        return Mono.fromRunnable(() -> executionMapper.updateById(exec))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /** 异步累加画布每日执行统计，失败不影响主执行链路。 */
    private void incrementStats(Long canvasId, int finalStatus, String userId) {
        Thread.ofVirtual().start(() -> {
            try {
                statsMapper.upsertDailyIncrement(canvasId, java.time.LocalDate.now(), finalStatus);
            } catch (Exception e) {
                log.warn("[STATS] 更新统计失败 canvasId={}: {}", canvasId, e.getMessage());
            }
        });
    }
}
