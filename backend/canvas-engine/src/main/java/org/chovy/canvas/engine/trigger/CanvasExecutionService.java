package org.chovy.canvas.engine.trigger;

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
import org.chovy.canvas.perf.PerfRunContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;
    private final CanvasExecutionMapper executionMapper;
    private final CanvasConfigCache configCache;
    private final DagParser dagParser;
    private final ContextPersistenceService ctxStore;
    private final DagEngine dagEngine;
    private final TriggerPreCheckService preCheckService;
    private final InFlightExecutionRegistry executionRegistry;
    private final org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper statsMapper;
    private final CanvasEntityCache canvasEntityCache;
    private final MqTriggerHandler mqTriggerHandler;
    private final CanvasExecutionDlqMapper dlqMapper;
    private final TriggerPriorityConfig priorityConfig;
    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;
    private final CdpUserService cdpUserService;

    @Value("${canvas.execution.context-ttl-sec:86400}")
    private long ctxTtlSec;

    @Value("${canvas.execution.global-timeout-sec:600}")
    private long globalTimeoutSec;

    @org.springframework.beans.factory.annotation.Value("${canvas.execution.max-concurrency:1000}")
    private int globalMaxConcurrency;

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
                payload, msgId, dryRun, false, 0, false);
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
        return triggerInternal(canvasId, userId, triggerType, triggerNodeType, matchKey,
                payload, msgId, false, false, 0, true);
    }

    /**
     * Internal Disruptor entrypoint. The dispatch options token is not publicly
     * constructible, so business payloads cannot forge overflow-retry status.
     */
    public Mono<Map<String, Object>> triggerFromDisruptor(
            Long canvasId, String userId, String triggerType,
            String triggerNodeType, String matchKey,
            Map<String, Object> payload, String msgId,
            CanvasDisruptorService.DispatchOptions dispatchOptions) {
        boolean overflowRetry = dispatchOptions != null && dispatchOptions.isOverflowRetry();
        int overflowChainRetryCount = dispatchOptions != null
                ? dispatchOptions.getOverflowChainRetryCount()
                : 0;
        return triggerInternal(canvasId, userId, triggerType, triggerNodeType, matchKey,
                payload, msgId, false, overflowRetry, overflowChainRetryCount, false);
    }

    private Mono<Map<String, Object>> triggerInternal(
            Long canvasId, String userId, String triggerType,
            String triggerNodeType, String matchKey,
            Map<String, Object> payload, String msgId, boolean dryRun,
            boolean overflowRetry, int overflowChainRetryCount,
            boolean persistentRequest) {
        return Mono.fromCallable(() -> {

                    CanvasDO canvas = canvasEntityCache.get(canvasId);
                    if (canvas == null) {
                        throw new IllegalStateException("画布不存在: " + canvasId);
                    }
                    if (!dryRun && !Objects.equals(canvas.getStatus(), CanvasStatusEnum.PUBLISHED.getCode())) {
                        throw new IllegalStateException("画布未发布，请先发布后再触发: " + canvasId);
                    }

                    boolean isResume = !dryRun && ctxStore.exists(canvasId, userId);
                    String acquiredDedupKey = null;
                    if (!dryRun && !overflowRetry && msgId != null) {
                        Duration dedupTtl = isResume
                                ? Duration.ofSeconds(globalTimeoutSec + 600)
                                : Duration.ofHours(24);
                        if (!ctxStore.acquireDedup(canvasId, userId, msgId, dedupTtl)) {
                            log.debug("[ENGINE] dedup 拦截 canvasId={} userId={} msgId={}", canvasId, userId, msgId);
                            return Map.<String, Object>of(MapFieldKeys.DEDUPLICATED, true);
                        }
                        acquiredDedupKey = ctxStore.buildDedupKey(canvasId, userId, msgId);
                    }

                    if (!dryRun) {
                        preCheckService.checkWithoutQuotaAccounting(canvas, userId);
                        if (overflowRetry) {
                            log.debug("[ENGINE] 溢出重试先跳过配额扣减 canvasId={} userId={}", canvasId, userId);
                        }
                    }

                    int admissionLimit = globalMaxConcurrency;
                    if (!dryRun) {
                        TriggerPriorityConfig.Priority priority = priorityConfig.of(triggerType);
                        int active = executionRegistry.activeCount(canvasId);
                        int maxConc = canvas.getMaxTotalExecutions() != null
                                ? Math.min(canvas.getMaxTotalExecutions(), globalMaxConcurrency)
                                : globalMaxConcurrency;
                        admissionLimit = maxConc;
                        if (priority == TriggerPriorityConfig.Priority.HIGH) {
                            int highMax = Math.max(1, (int) (maxConc * priorityConfig.getHighMaxConcurrencyRatio()));
                            admissionLimit = globalMaxConcurrency;
                            if (active >= highMax) {
                                log.error("[ENGINE] HIGH优先级超并发告警 canvasId={} active={}/{}",
                                        canvasId, active, highMax);
                            }
                        } else {
                            int effectiveMax = priority == TriggerPriorityConfig.Priority.LOW
                                    ? Math.max(1, (int) (maxConc * priorityConfig.getLowRatio()))
                                    : maxConc;
                            admissionLimit = effectiveMax;

                            if (active >= effectiveMax) {
                                log.warn("[ENGINE] 并发上限 canvasId={} active={}/{} priority={}",
                                        canvasId, active, effectiveMax, priority);
                                if (priority == TriggerPriorityConfig.Priority.NORMAL) {
                                    if (persistentRequest) {
                                        return Map.of(MapFieldKeys.OVERFLOW, "request_retry");
                                    }
                                    boolean queued = sendOverflowRetry(canvasId, userId, triggerType, triggerNodeType,
                                            matchKey, payload, msgId, overflowChainRetryCount);
                                    return Map.of(MapFieldKeys.OVERFLOW, queued ? "queued_for_retry" : "retry_enqueue_failed");
                                }
                                return Map.of(MapFieldKeys.OVERFLOW, "dropped_low_priority");
                            }
                        }
                    }

                    ExecutionContext ctx;
                    boolean resumeLockAcquired = false;
                    try {
                        if (isResume && !dryRun) {
                            String instanceId = UUID.randomUUID().toString();
                            if (!ctxStore.acquireResumeLock(canvasId, userId, instanceId, globalTimeoutSec)) {
                                log.warn("[ENGINE] resume-lock 竞争失败，放弃本次触发 canvasId={}", canvasId);
                                return Map.<String, Object>of(MapFieldKeys.SKIPPED, "resume-lock");
                            }
                            resumeLockAcquired = true;
                            ctx = ctxStore.load(canvasId, userId);
                            if (ctx == null) {
                                ctx = newContext(canvasId, resolveVersionId(canvas, userId, false), userId, triggerType);
                            }
                        } else {
                            ctx = newContext(canvasId, resolveVersionId(canvas, userId, dryRun), userId, triggerType);
                        }

                        ctx.setTriggerType(triggerType);
                        ctx.setTriggerNodeType(triggerNodeType);
                        ctx.setMatchKey(matchKey);
                        ctx.getTriggerPayload().putAll(sanitizePayload(payload));
                        ctx.setPerfRunId(PerfRunContext.extract(ctx.getTriggerPayload()));

                        DagGraph graph = configCache.get(canvasId, ctx.getVersionId());
                        String triggerNodeId = findTriggerNode(graph, triggerNodeType, matchKey);
                        if (triggerNodeId == null) {
                            throw new IllegalStateException(
                                    "找不到触发器节点 type=" + triggerNodeType + " key=" + matchKey);
                        }

                        Map<String, Object> prep = new HashMap<>();
                        prep.put(MapFieldKeys.CTX, ctx);
                        prep.put(MapFieldKeys.GRAPH, graph);
                        prep.put(MapFieldKeys.TRIGGER_NODE_ID, triggerNodeId);
                        prep.put(MapFieldKeys.IS_RESUME, isResume);
                        prep.put(MapFieldKeys.CANVAS, canvas);
                        prep.put(MapFieldKeys.ADMISSION_LIMIT, admissionLimit);
                        if (acquiredDedupKey != null) prep.put(MapFieldKeys.DEDUP_KEY, acquiredDedupKey);
                        return prep;
                    } catch (RuntimeException e) {
                        if (resumeLockAcquired) {
                            ctxStore.releaseResumeLock(canvasId, userId);
                        }
                        if (acquiredDedupKey != null) {
                            ctxStore.releaseDedup(acquiredDedupKey);
                        }
                        throw e;
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(prep -> {
                    if (!prep.containsKey(MapFieldKeys.CTX)) {
                        return Mono.just(new HashMap<String, Object>(prep));
                    }
                    ExecutionContext ctx = (ExecutionContext) prep.get(MapFieldKeys.CTX);
                    DagGraph graph = (DagGraph) prep.get(MapFieldKeys.GRAPH);
                    String triggerNodeId = (String) prep.get(MapFieldKeys.TRIGGER_NODE_ID);
                    boolean isResume = (Boolean) prep.get(MapFieldKeys.IS_RESUME);
                    CanvasDO canvas = (CanvasDO) prep.get(MapFieldKeys.CANVAS);
                    String acquiredDedupKey = (String) prep.get(MapFieldKeys.DEDUP_KEY);
                    int admissionLimit = (Integer) prep.get(MapFieldKeys.ADMISSION_LIMIT);

                    Disposable.Swap executionSlot = null;
                    if (!dryRun) {
                        var acquired = executionRegistry.tryAcquire(
                                canvasId, ctx.getExecutionId(), admissionLimit, globalMaxConcurrency);
                        if (acquired.isEmpty()) {
                            int active = executionRegistry.activeCount(canvasId);
                            log.warn("[ENGINE] 画布并发上限已达 canvasId={} active={}/{} global={}/{}",
                                    canvasId, active, admissionLimit,
                                    executionRegistry.totalActiveCount(), globalMaxConcurrency);
                            if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId());
                            if (acquiredDedupKey != null) ctxStore.releaseDedup(acquiredDedupKey);
                            return Mono.just(Map.of(MapFieldKeys.OVERFLOW, "concurrency_limit_reached",
                                    MapFieldKeys.ACTIVE, active, MapFieldKeys.LIMIT, admissionLimit));
                        }
                        executionSlot = acquired.get();
                    }
                    final Disposable.Swap finalExecutionSlot = executionSlot;

                    if (!dryRun) {
                        try {
                            preCheckService.consumeQuotaAndRecord(canvas, userId);
                        } catch (RuntimeException e) {
                            if (finalExecutionSlot != null) {
                                executionRegistry.deregister(canvasId, ctx.getExecutionId());
                            }
                            if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId());
                            if (acquiredDedupKey != null) ctxStore.releaseDedup(acquiredDedupKey);
                            return Mono.error(e);
                        }
                    }

                    ensureCdpUser(ctx);
                    final CanvasExecutionDO finalExec = createExecution(ctx);
                    if (ctx.getPerfRunId() != null && acquiredDedupKey != null) {
                        finalExec.setLastDedupKey(acquiredDedupKey);
                    }
                    Mono<Map<String, Object>> executionMono = dagEngine.execute(graph, triggerNodeId, ctx)
                            .timeout(Duration.ofSeconds(globalTimeoutSec));

                    Mono<Map<String, Object>> runMono = insertExecution(finalExec).then(executionMono
                            .doOnSubscribe(sub -> {
                                if (finalExecutionSlot != null) {
                                    finalExecutionSlot.update(sub::cancel);
                                }
                            })
                            .flatMap(result -> {
                                // 9. 执行完成或挂起，更新执行记录
                                boolean paused = isPaused(ctx, graph);
                                if (paused) {
                                    if (!dryRun) {
                                        ctxStore.save(ctx);
                                        if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId());
                                        incrementStats(ctx.getCanvasId(), ExecutionStatus.PAUSED.getCode(), ctx.getUserId());
                                    }
                                    Map<String, Object> resp = new HashMap<>(result);
                                    resp.put(MapFieldKeys.EXECUTION_ID, ctx.getExecutionId());
                                    return updateExecution(finalExec, ExecutionStatus.PAUSED.getCode(), result)
                                            .thenReturn(resp);
                                }
                                if (!dryRun) {
                                    ctxStore.delete(ctx.getCanvasId(), ctx.getUserId());
                                    if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId());
                                    incrementStats(ctx.getCanvasId(), ExecutionStatus.SUCCESS.getCode(), ctx.getUserId());
                                }
                                Map<String, Object> resp = new HashMap<>(result);
                                resp.put(MapFieldKeys.EXECUTION_ID, ctx.getExecutionId());
                                return updateExecution(finalExec, ExecutionStatus.SUCCESS.getCode(), result)
                                        .thenReturn(resp);
                            })
                            .onErrorResume(e -> {
                                log.error("[ENGINE] 执行失败 executionId={}: {}", ctx.getExecutionId(), e.getMessage());
                                boolean paused = isPaused(ctx, graph);
                                Mono<Void> updateMono;
                                if (paused) {
                                    if (!dryRun) {
                                        ctxStore.save(ctx);
                                        if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId());
                                    }
                                    updateMono = updateExecution(finalExec, ExecutionStatus.PAUSED.getCode(), Map.of());
                                    if (!dryRun) incrementStats(ctx.getCanvasId(), ExecutionStatus.PAUSED.getCode(), ctx.getUserId());
                                } else {
                                    if (!dryRun) {
                                        ctxStore.delete(ctx.getCanvasId(), ctx.getUserId());
                                        if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId());
                                    }
                                    updateMono = updateExecution(finalExec, ExecutionStatus.FAILED.getCode(),
                                            Map.of(MapFieldKeys.ERROR, e.getMessage()));
                                    if (!dryRun) incrementStats(ctx.getCanvasId(), ExecutionStatus.FAILED.getCode(), ctx.getUserId());
                                }
                                Map<String, Object> resp = Map.of(
                                        MapFieldKeys.ERROR, e.getMessage(),
                                        MapFieldKeys.EXECUTION_ID, ctx.getExecutionId()
                                );
                                return updateMono.thenReturn(resp);
                            }));
                    return runMono.doOnError(e -> {
                        if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId());
                        if (acquiredDedupKey != null) ctxStore.releaseDedup(acquiredDedupKey);
                    }).doFinally(signal -> {
                        if (!dryRun) executionRegistry.deregister(canvasId, ctx.getExecutionId());
                    });
                });
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

    private void ensureCdpUser(ExecutionContext ctx) {
        if (ctx.getUserId() != null && !ctx.getUserId().isBlank()) {
            cdpUserService.ensureUser(ctx.getUserId(), "CANVAS_EXECUTION", ctx.getExecutionId());
        }
    }

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

    private String findTriggerNode(DagGraph graph, String triggerNodeType, String matchKey) {
        log.debug("[FIND_TRIGGER] triggerNodeType={} matchKey={}", triggerNodeType, matchKey);

        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || !triggerNodeType.equals(node.getType())) continue;
            if (NodeType.WAIT.equals(triggerNodeType) || NodeType.GOAL_CHECK.equals(triggerNodeType)) {
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

    private boolean isPaused(ExecutionContext ctx, DagGraph graph) {
        return ctx.getNodeStatuses().values().stream()
                .anyMatch(s -> s == org.chovy.canvas.engine.context.NodeStatus.WAITING);
    }

    private boolean sendOverflowRetry(Long canvasId, String userId, String triggerType,
                                      String triggerNodeType, String matchKey,
                                      Map<String, Object> payload, String msgId, int chainRetryCount) {
        Map<String, Object> retryPayload = sanitizePayload(payload);
        try {
            String effectiveMsgId = nonBlank(msgId)
                    ? msgId
                    : "overflow-" + UUID.randomUUID();
            OverflowRetryMessage msg = new OverflowRetryMessage(
                    canvasId, userId, triggerType, triggerNodeType,
                    matchKey, retryPayload, effectiveMsgId, chainRetryCount + 1);
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

    private Map<String, Object> sanitizePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new HashMap<>(payload);
        sanitized.remove(OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY);
        return sanitized;
    }

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

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.substring(0, Math.min(value.length(), maxLength));
    }

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

    private Mono<Void> insertExecution(CanvasExecutionDO exec) {
        return Mono.fromRunnable(() -> executionMapper.insert(exec))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

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
