package org.chovy.canvas.engine.trigger;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.config.CanvasRuntimeMetrics;
import org.chovy.canvas.config.CorrelationIdWebFilter;
import org.chovy.canvas.config.ExecutionLaneProperties;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.rule.CanvasRuleGraphValidator;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.engine.trace.ExecutionTraceContext;
import org.chovy.canvas.infrastructure.concurrent.ManagedVirtualThreadExecutor;
import org.chovy.canvas.infrastructure.observability.MdcTaskDecorator;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import jakarta.annotation.PostConstruct;
import org.chovy.canvas.perf.PerfRunContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
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

    /** 画布执行记录 Mapper。 */
    private final CanvasExecutionMapper executionMapper;
    /** 画布配置加载器。 */
    private final CanvasExecutionConfigLoader configLoader;
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
    /** 触发入场决策服务。 */
    private final TriggerAdmissionService triggerAdmissionService;
    /** 执行 lane 预算配置。 */
    private final ExecutionLaneProperties executionLaneProperties;
    /** 执行 lane 派发器。 */
    private final ExecutionLaneDispatcher executionLaneDispatcher;
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
    private ManagedVirtualThreadExecutor backgroundExecutor = ManagedVirtualThreadExecutor.direct();
    private ExecutionLifecycleGate lifecycleGate = new ExecutionLifecycleGate(0);
    private CanvasRuntimeMetrics runtimeMetrics;
    private CanvasRuleGraphValidator canvasRuleGraphValidator;

    @Autowired(required = false)
    void setBackgroundExecutor(ManagedVirtualThreadExecutor backgroundExecutor) {
        this.backgroundExecutor = backgroundExecutor;
    }

    @Autowired
    void setLifecycleGate(ExecutionLifecycleGate lifecycleGate) {
        this.lifecycleGate = lifecycleGate;
    }

    @Autowired(required = false)
    void setRuntimeMetrics(CanvasRuntimeMetrics runtimeMetrics) {
        this.runtimeMetrics = runtimeMetrics;
    }

    @Autowired(required = false)
    void setCanvasRuleGraphValidator(CanvasRuleGraphValidator canvasRuleGraphValidator) {
        this.canvasRuleGraphValidator = canvasRuleGraphValidator;
    }

    /** 执行上下文在 Redis 中的保留秒数。 */
    @Value("${canvas.execution.context-ttl-sec:86400}")
    private long ctxTtlSec;

    /** 单次画布执行全局超时时间。 */
    @Value("${canvas.execution.global-timeout-sec:600}")
    private long globalTimeoutSec;

    /** 全局最大并发执行数。 */
    @org.springframework.beans.factory.annotation.Value("${canvas.execution.max-concurrency:3000}")
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

    /**
     * 执行 trigger Dry Run 对应的业务逻辑。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param userId userId 对应的业务主键或标识
     * @param payload payload 请求体、消息体或事件载荷
     * @param graphJson graphJson 方法执行所需的业务参数
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
// ── 触发入口 ──────────────────────────────────────────────────

    /**
     * dry-run 专用入口：直接传入 graphJson，跳过 DB draft 读取。
     * 解决"配置未保存到 DB 时，dry-run 用旧数据"的问题。
     */
    public Mono<Map<String, Object>> triggerDryRun(
            Long canvasId, String userId,
            Map<String, Object> payload, String graphJson) {
        Mono<Map<String, Object>> execution = Mono.fromCallable(MdcTaskDecorator.decorate(() -> {
                    CanvasDO canvas = configLoader.loadCanvasForDryRun(canvasId);

                    ExecutionContext ctx = newContext(canvasId, -1L, userId, TriggerType.DRY_RUN);
                    ctx.setTenantId(canvas.getTenantId());
                    ctx.putTriggerPayloadValues(triggerAdmissionService.sanitizePayload(payload));
                    ctx.setPerfRunId(PerfRunContext.extract(ctx.getTriggerPayload()));

                    DagGraph graph;
                    if (graphJson != null && !graphJson.isBlank()) {
                        graph = configLoader.parseGraph(graphJson);
                    } else {
                        Long versionId = configLoader.resolveVersionId(canvas, userId, true);
                        graph = configLoader.loadGraph(canvasId, versionId);
                        ctx.setVersionId(versionId);
                    }
                    validateGraphForDryRun(graph);

                    String triggerNodeId = configLoader.findTriggerNode(graph, NodeType.DIRECT_CALL, null);
                    if (triggerNodeId == null) {
                        triggerNodeId = graph.entryNodes().stream().findFirst().orElse(null);
                    }
                    if (triggerNodeId == null)
                        throw new IllegalStateException("画布没有入口节点，请确保存在触发器节点");

                    return Map.of(
                            MapFieldKeys.CTX, ctx,
                            MapFieldKeys.GRAPH, graph,
                            MapFieldKeys.TRIGGER_NODE_ID, triggerNodeId);
                }))
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

                    ExecutionTraceContext traceContext = ExecutionTraceContext.from(ctx);
                    Mono<Map<String, Object>> dryRunExecution = insertExecution(exec)
                            .then(traceContext.scope(dagEngine.execute(graph, triggerNodeId, ctx))
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
                    return traceContext.scope(dryRunExecution);
                });
        return lifecycleGate.guard(execution);
    }

    /**
     * 触发画布执行：执行工作流包含 dedup 检查、上下文准备、DAG 执行以及结果持久化。
     */
    public Mono<Map<String, Object>> trigger(
            Long canvasId, String userId, String triggerType,
            String triggerNodeType, String matchKey,
            Map<String, Object> payload, String msgId, boolean dryRun) {
        return triggerInternal(canvasId, userId, triggerType, triggerNodeType, matchKey,
                payload, msgId, dryRun, false, 0, false, 0, null, null);
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
                payload, msgId, false, false, 0, true, priorAttemptCount, lastError, null);
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
        return triggerFromDisruptor(canvasId, userId, triggerType, triggerNodeType,
                matchKey, payload, msgId, null, dispatchOptions);
    }

    /**
     * Internal Disruptor entrypoint with pre-resolved lane metadata.
     */
    public Mono<Map<String, Object>> triggerFromDisruptor(
            Long canvasId,
            String userId,
            String triggerType,
            String triggerNodeType,
            String matchKey,
            Map<String, Object> payload,
            String msgId,
            ExecutionLane executionLane,
            CanvasDisruptorService.DispatchOptions dispatchOptions
    ) {
        boolean overflowRetry = dispatchOptions != null && dispatchOptions.isOverflowRetry();
        int overflowChainRetryCount = dispatchOptions != null
                ? dispatchOptions.getOverflowChainRetryCount()
                : 0;
        return triggerInternal(canvasId, userId, triggerType, triggerNodeType, matchKey,
                payload, msgId, false, overflowRetry, overflowChainRetryCount, false, 0, null, executionLane);
    }

    /** 统一触发入口，串联准备阶段和执行阶段的响应式流程。 */
    private Mono<Map<String, Object>> triggerInternal(
            Long canvasId, String userId, String triggerType,
            String triggerNodeType, String matchKey,
            Map<String, Object> payload, String msgId, boolean dryRun,
            boolean overflowRetry, int overflowChainRetryCount,
            boolean persistentRequest, int priorAttemptCount, String lastError,
            ExecutionLane executionLaneOverride) {
        Mono<Map<String, Object>> execution = Mono.fromCallable(MdcTaskDecorator.decorate(() ->
                        // 校验与准备
                        prepareExecution(
                                canvasId, userId, triggerType,
                                triggerNodeType, matchKey, payload,
                                msgId, dryRun, overflowRetry,
                                overflowChainRetryCount, persistentRequest,
                                priorAttemptCount, lastError,
                                executionLaneOverride)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(
                        // 执行阶段
                        prep -> executeFromPrep(canvasId, userId, dryRun, prep));
        return lifecycleGate.guard(execution);
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
        Object laneValue = prep.get(MapFieldKeys.EXECUTION_LANE);
        ExecutionLane executionLane = laneValue instanceof ExecutionLane lane ? lane : ExecutionLane.STANDARD;
        int laneLimit = executionLaneProperties.limitFor(executionLane);

        // 执行槽位是最终并发卡口，失败时会释放已获取的 resumeLock/dedupKey。
        ExecutionLaneDispatcher.SlotAcquisitionResult slotResult = executionLaneDispatcher.tryAcquireSlot(
                canvasId, ctx, executionLane, admissionLimit, laneLimit, globalMaxConcurrency,
                dryRun, isResume, acquiredDedupKey);
        if (slotResult.isOverflow()) return slotResult.overflowMono();

        ExecutionTraceContext traceContext = ExecutionTraceContext.from(ctx);
        try (ExecutionTraceContext.Scope ignored = traceContext.open()) {
            return traceContext.scope(doExecute(canvasId, userId, dryRun,
                    canvas, slotResult.slot(), ctx,
                    isResume, acquiredDedupKey,
                    graph, triggerNodeId));
        }
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
                // 配额扣减发生在 DAG 前；失败必须同步释放 slot 和分布式锁。
                consumeQuotaIfNeeded(canvas, userId, ctx);
            } catch (RuntimeException e) {
                releaseResources(canvasId, finalExecutionSlot, ctx, isResume, acquiredDedupKey);
                return Mono.error(e);
            }
        }

        // 2. 保存 CDP 用户相关信息
        ensureCdpUser(ctx);
        final CanvasExecutionDO finalExec = buildExecutionRecord(ctx, acquiredDedupKey, canvas.getTenantId());

        // 3. 运行 DAG
        Mono<Map<String, Object>> executionMono = dagEngine.execute(graph, triggerNodeId, ctx)
                .timeout(Duration.ofSeconds(globalTimeoutSec));

        // 4. 组装响应式链：持久化记录 → 绑定取消句柄 → 处理结果/错误 → 释放分布式锁
        return persistExecutionStart(finalExec, isResume)
                .then(executionMono
                        .doOnSubscribe(sub -> {
                            // 绑定取消句柄，Kill Switch 可通过 registry 取消当前 Reactor 订阅。
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
        if (!ctx.isQuotaBypass() && !TriggerAdmissionService.isInternalContinuationTrigger(ctx.getTriggerType())) {
            preCheckService.consumeQuotaAndRecord(canvas, userId);
        }
    }

    /** 基于执行上下文创建执行记录，并在压测链路保存实际获取的幂等键。 */
    private CanvasExecutionDO buildExecutionRecord(ExecutionContext ctx, String acquiredDedupKey, Long tenantId) {
        ctx.setTenantId(tenantId);
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
                // WAITING 表示执行挂起，保留 ctx 快照供后续恢复触发继续推进。
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
            // 无挂起节点时清理 ctx，避免下一次正常触发被误判为恢复执行。
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
        recordExecutionFailure(ctx, e);
        boolean paused = isPaused(ctx, graph);
        Mono<Void> updateMono;
        if (paused) {
            if (!dryRun) {
                // 异常后仍存在 WAITING 节点时按 PAUSED 落库，保留恢复机会。
                ctxStore.save(ctx);
                if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId(), ctx.getResumeLockToken());
            }
            updateMono = updateExecution(finalExec, ExecutionStatus.PAUSED.getCode(), Map.of());
            if (!dryRun)
                incrementStats(ctx.getCanvasId(), ExecutionStatus.PAUSED.getCode(), ctx.getUserId());
        } else {
            if (!dryRun) {
                // 非挂起失败是终态，删除 ctx 防止旧上下文污染后续触发。
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
    private Map<String, ?> prepareExecution(Long canvasId, String userId, String triggerType, String triggerNodeType, String matchKey, Map<String, Object> payload, String msgId, boolean dryRun, boolean overflowRetry, int overflowChainRetryCount, boolean persistentRequest, int priorAttemptCount, String lastError, ExecutionLane executionLaneOverride) {

        // A. 从 Caffeine L1 缓存加载画布实体，验证存在且已发布
        //    ⚠️ 分布式：L1 是 JVM 本地，画布下线后最多延迟一个缓存 TTL 才感知
        CanvasDO canvas = configLoader.validateAndLoadCanvas(canvasId, dryRun);
        TriggerAdmissionService.AdmissionDecision admission = triggerAdmissionService.evaluate(
                new TriggerAdmissionService.AdmissionRequest(
                        canvasId, userId, triggerType,
                        triggerNodeType, matchKey, payload,
                        msgId, dryRun, overflowRetry,
                        overflowChainRetryCount, persistentRequest,
                        priorAttemptCount, canvas,
                        globalMaxConcurrency, globalTimeoutSec,
                        executionLaneOverride));
        if (admission.isShortCircuited()) {
            return admission.shortCircuitResponse();
        }

        // E. 创建/恢复执行上下文，resumeLock 跨机安全（Redis SETNX）
        return buildPrepMap(
                canvasId, userId, triggerType,
                triggerNodeType, matchKey, payload,
                msgId, dryRun, admission.isResume(), canvas,
                admission.admissionLimit(), admission.dedupKey(), admission.quotaBypass(),
                admission.executionLane());
    }

    /**
     * 创建或恢复执行上下文，并构建 prep Map。
     *
     * <p>两条路径：
     * <ul>
     *   <li>isResume=true（Redis 中有上下文快照）：
     *       先抢 resumeLock（Redis SETNX，防止多个触发并发恢复同一份上下文），
     *       再从 Redis 反序列化上下文；若 Redis 快照丢失则拒绝恢复，避免错误新建执行；</li>
     *   <li>isResume=false：直接创建新的 {@link ExecutionContext}。</li>
     * </ul>
     *
     * <p>resumeLock 分布式语义（跨机安全）：
     * 同一时刻只有一个机器实例能持有 (canvasId, userId) 的 resumeLock，
     * 其他机器抢锁失败时直接放弃本次触发（SKIPPED）。
     * 锁的 TTL = globalTimeoutSec，保证执行超时后锁自动释放。
     */
    private Map<String, Object> buildPrepMap(Long canvasId, String userId, String triggerType, String triggerNodeType, String matchKey, Map<String, Object> payload, String msgId, boolean dryRun, boolean isResume, CanvasDO canvas, int admissionLimit, String acquiredDedupKey, boolean quotaBypass, ExecutionLane executionLane) {
        boolean resumeLockAcquired = false;
        // 提升到 try 外层，使 catch 块可以用于原子释放锁
        String resumeLockInstanceId = null;
        ExecutionContext ctx;

        try {

            if (isResume && !dryRun) {
                resumeLockInstanceId = UUID.randomUUID().toString();
                if (!ctxStore.acquireResumeLock(canvasId, userId, resumeLockInstanceId, globalTimeoutSec)) {
                    // 同一用户同一画布只允许一个恢复执行持有上下文，避免并发恢复写乱 ctx。
                    log.warn("[ENGINE] resume-lock 竞争失败，放弃本次触发 canvasId={}", canvasId);
                    releaseDedupIfPresent(acquiredDedupKey);
                    return Map.<String, Object>of(MapFieldKeys.SKIPPED, "resume-lock");
                }
                resumeLockAcquired = true;
                ctx = ctxStore.load(canvasId, userId);
                if (ctx == null) {
                    triggerAdmissionService.markMissingResumeContext(payload, msgId);
                    ctxStore.releaseResumeLock(canvasId, userId, resumeLockInstanceId);
                    releaseDedupIfPresent(acquiredDedupKey);
                    return Map.of(
                            MapFieldKeys.SKIPPED, "missing-context",
                            MapFieldKeys.ERROR, "internal continuation context is missing");
                }
                // 存入 ctx，后续 doExecute / handleSuccess / handleError 用于原子释放锁
                ctx.setResumeLockToken(resumeLockInstanceId);
            } else {
                ctx = newContext(canvasId, configLoader.resolveVersionId(canvas, userId, dryRun), userId, triggerType);
            }


            // 补充上下文信息
            ctx.setTenantId(canvas.getTenantId());
            populateContext(triggerType, triggerNodeType, matchKey, payload, ctx);
            ctx.setQuotaBypass(quotaBypass);
            ctx.putTriggerPayloadValues(Map.of(MapFieldKeys.EXECUTION_LANE, executionLane.name()));


            // 查找画布内容
            DagGraph graph = configLoader.loadGraph(canvasId, ctx.getVersionId());

            // 查找触发器节点
            String triggerNodeId = configLoader.findTriggerNode(graph, triggerNodeType, matchKey);
            if (triggerNodeId == null) {
                throw new IllegalStateException(
                        "找不到触发器节点 type=" + triggerNodeType + " key=" + matchKey);
            }

            // 构建返回结果
            return buildPrepareResultMap(
                    isResume, canvas,
                    admissionLimit, acquiredDedupKey,
                    executionLane, ctx, graph, triggerNodeId
            );
        } catch (RuntimeException e) {
            if (resumeLockAcquired) {
                // 使用 instanceId 原子释放（Lua check-then-del，防误删他机的锁）
                ctxStore.releaseResumeLock(canvasId, userId, resumeLockInstanceId);
            }
            releaseDedupIfPresent(acquiredDedupKey);
            throw e;
        }
    }

    private void releaseDedupIfPresent(String acquiredDedupKey) {
        if (acquiredDedupKey != null) {
            ctxStore.releaseDedup(acquiredDedupKey);
        }
    }

    private void validateGraphForDryRun(DagGraph graph) {
        if (canvasRuleGraphValidator != null) {
            canvasRuleGraphValidator.validateOrThrow(graph);
        }
    }

    /** 构建准备阶段输出 Map，供执行阶段解包上下文、图和准入信息。 */
    private static Map<String, Object> buildPrepareResultMap(boolean isResume, CanvasDO canvas, int admissionLimit, String acquiredDedupKey, ExecutionLane executionLane, ExecutionContext ctx, DagGraph graph, String triggerNodeId) {
        Map<String, Object> prep = new HashMap<>();
        prep.put(MapFieldKeys.CTX, ctx);
        prep.put(MapFieldKeys.GRAPH, graph);
        prep.put(MapFieldKeys.TRIGGER_NODE_ID, triggerNodeId);
        prep.put(MapFieldKeys.IS_RESUME, isResume);
        prep.put(MapFieldKeys.CANVAS, canvas);
        prep.put(MapFieldKeys.ADMISSION_LIMIT, admissionLimit);
        prep.put(MapFieldKeys.EXECUTION_LANE, executionLane);
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
        ctx.putTriggerPayloadValues(triggerAdmissionService.sanitizePayload(payload));
        putCurrentTraceId(ctx);
        ctx.setPerfRunId(PerfRunContext.extract(ctx.getTriggerPayload()));
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
            boolean queued = triggerAdmissionService.enqueueOverflowRetry(canvasId, userId, TriggerType.EVENT,
                    NodeType.EVENT_TRIGGER, eventCode, payload, msgId, 0);
            if (!queued) {
                log.error("[EVENT] 溢出事件入队失败，触发丢失 canvasId={} eventCode={}", canvasId, eventCode);
            }
        }
    }

    /**
     * 删除、清理或失效 invalidate Canvas 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     */
// ── 缓存失效 ──────────────────────────────────────────────────

    /**
     * 画布发布/下线时主动驱逐 CanvasDO 实体缓存，
     * 确保下次 trigger() 读到最新状态（已发布/已下线）。
     */
    public void invalidateCanvas(Long canvasId) {
        try {
            configLoader.invalidateCanvas(canvasId);
        } catch (RuntimeException e) {
            if (runtimeMetrics != null) {
                runtimeMetrics.recordCacheInvalidationFailure("canvas_entity", e.getClass().getSimpleName());
            }
            throw e;
        }
    }

    private void recordExecutionFailure(ExecutionContext ctx, Throwable e) {
        if (runtimeMetrics == null) {
            return;
        }
        String triggerType = ctx != null ? ctx.getTriggerType() : null;
        String reason = e != null ? e.getClass().getSimpleName() : null;
        runtimeMetrics.recordExecutionFailure(triggerType, reason);
    }

    /**
     * 执行 new Context 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param versionId versionId 对应的业务主键或标识
     * @param userId userId 对应的业务主键或标识
     * @param triggerType triggerType 类型标识或分类条件
     * @return 方法执行后的业务结果
     */
// ── 私有帮助方法 ──────────────────────────────────────────────

    /** 创建新的执行上下文并写入基础画布、版本、用户和触发信息。 */
    private ExecutionContext newContext(Long canvasId, Long versionId, String userId, String triggerType) {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId(UUID.randomUUID().toString());
        ctx.setCanvasId(canvasId);
        ctx.setVersionId(versionId);
        ctx.setUserId(userId);
        ctx.setTriggerType(triggerType);
        if (userId != null) ctx.putTriggerPayloadValues(Map.of(MapFieldKeys.USER_ID, userId));
        putCurrentTraceId(ctx);
        return ctx;
    }

    private void putCurrentTraceId(ExecutionContext ctx) {
        CorrelationIdWebFilter.currentTraceId()
                .ifPresent(traceId -> ctx.putTriggerPayloadValues(Map.of(CorrelationIdWebFilter.MDC_KEY, traceId)));
    }

    /** 确保执行上下文中的用户已写入 CDP 用户画像。 */
    private void ensureCdpUser(ExecutionContext ctx) {
        if (CanvasSchedulerService.isScheduledBatchUser(ctx.getUserId())) {
            return;
        }
        if (StrUtil.isNotBlank(ctx.getUserId())) {
            cdpUserService.ensureUser(ctx.getTenantId(), ctx.getUserId(), "CANVAS_EXECUTION", ctx.getExecutionId());
        }
    }

    /** 判断执行上下文中是否存在 WAITING 节点状态。 */
    private boolean isPaused(ExecutionContext ctx, DagGraph graph) {
        return ctx.getNodeStatuses().values().stream()
                .anyMatch(s -> s == org.chovy.canvas.engine.context.NodeStatus.WAITING);
    }

    /** 根据执行上下文创建 RUNNING 状态的画布执行记录。 */
    private CanvasExecutionDO createExecution(ExecutionContext ctx) {
        CanvasExecutionDO exec = new CanvasExecutionDO();
        exec.setId(ctx.getExecutionId());
        exec.setTenantId(ctx.getTenantId());
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
        return Mono.defer(() -> Mono.fromRunnable(MdcTaskDecorator.decorate(
                        (Runnable) () -> executionMapper.insert(exec)))
                .subscribeOn(Schedulers.boundedElastic())
                .then());
    }

    /** 恢复旧 execution 时不能再次 INSERT 同一个主键，只需把 PAUSED 记录标回 RUNNING。 */
    private Mono<Void> persistExecutionStart(CanvasExecutionDO exec, boolean isResume) {
        if (!isResume) {
            return insertExecution(exec);
        }
        return markExecutionRunning(exec);
    }

    /** 将已挂起的执行记录重新置为 RUNNING，便于后续成功/失败/再次挂起更新同一条记录。 */
    private Mono<Void> markExecutionRunning(CanvasExecutionDO exec) {
        if (exec == null) return Mono.empty();
        CanvasExecutionDO update = new CanvasExecutionDO();
        update.setStatus(ExecutionStatus.RUNNING.getCode());
        return Mono.defer(() -> Mono.fromRunnable(MdcTaskDecorator.decorate((Runnable) () -> {
                    int updated = executionMapper.update(update,
                            new LambdaUpdateWrapper<CanvasExecutionDO>()
                                    .eq(CanvasExecutionDO::getId, exec.getId())
                                    .eq(CanvasExecutionDO::getStatus, ExecutionStatus.PAUSED.getCode()));
                    if (updated == 0) {
                        log.warn("[ENGINE] resume execution 未置为 RUNNING，可能状态已变化 executionId={}",
                                exec.getId());
                    }
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .then());
    }

    /** 更新执行状态和结果 JSON，序列化失败时保留状态更新。 */
    private Mono<Void> updateExecution(CanvasExecutionDO exec, int status, Map<String, Object> result) {
        if (exec == null) return Mono.empty();
        return updateExecutionById(exec.getId(), status, result);
    }

    /** 供特殊节点超时兜底等内部恢复路径更新已暂停的原 execution。 */
    public Mono<Void> completePausedExecution(ExecutionContext ctx, int status, Map<String, Object> result) {
        if (ctx == null || ctx.getExecutionId() == null) {
            return Mono.empty();
        }
        return updateExecutionById(ctx.getExecutionId(), status, result);
    }

    private Mono<Void> updateExecutionById(String executionId, int status, Map<String, Object> result) {
        CanvasExecutionDO update = new CanvasExecutionDO();
        update.setStatus(status);
        try {
            update.setResult(objectMapper.writeValueAsString(result));
        } catch (Exception ignored) {
        }
        return Mono.defer(() -> Mono.fromRunnable(MdcTaskDecorator.decorate((Runnable) () -> {
                    int updated = executionMapper.update(update,
                            new LambdaUpdateWrapper<CanvasExecutionDO>()
                                    .eq(CanvasExecutionDO::getId, executionId)
                                    .in(CanvasExecutionDO::getStatus,
                                            ExecutionStatus.RUNNING.getCode(),
                                            ExecutionStatus.PAUSED.getCode()));
                    if (updated == 0) {
                        log.warn("[ENGINE] execution 状态未更新，可能已被终止 executionId={} targetStatus={}",
                                executionId, status);
                    }
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .then());
    }

    /** 异步累加画布每日执行统计，失败不影响主执行链路。 */
    private void incrementStats(Long canvasId, int finalStatus, String userId) {
        backgroundExecutor.submit("canvas-stats-increment-" + canvasId, () -> {
            try {
                statsMapper.upsertDailyIncrement(canvasId, java.time.LocalDate.now(), finalStatus);
            } catch (Exception e) {
                log.warn("[STATS] 更新统计失败 canvasId={}: {}", canvasId, e.getMessage());
            }
        });
    }
}
