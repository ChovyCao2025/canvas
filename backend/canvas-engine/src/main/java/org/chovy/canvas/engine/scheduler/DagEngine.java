package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.execution.CanvasExecutionDlq;
import org.chovy.canvas.domain.execution.CanvasExecutionDlqMapper;
import org.chovy.canvas.domain.execution.CanvasExecutionTrace;
import org.chovy.canvas.domain.execution.CanvasExecutionTraceMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handler.HandlerRegistry;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.handlers.HubHandler;
import org.chovy.canvas.engine.handlers.LogicRelationHandler;
import org.chovy.canvas.infra.redis.ContextPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * DAG 执行调度器（精确实现设计文档第七章）。
 * 核心机制：
 * 1. 单节点 6 阶段执行（7.4节）
 * 2. repeat 并发保护（7.5节）—— 在写 SUCCESS 之前检查 repeat，防止幂等拦截
 * 3. LOGIC_RELATION AND 模式立即失败（7.7节）
 * 4. Hub 超时延迟任务（设计文档 Hub 定义）
 * 5. Priority 串行依序尝试（4.6节）
 * 6. 执行结束批量写入 SKIPPED（7.7节）
 */
@Slf4j
@Component
public class DagEngine {

    private final HandlerRegistry handlerRegistry;
    private final CanvasExecutionTraceMapper traceMapper;
    private final TraceWriteBuffer traceBuffer;
    private final CanvasExecutionDlqMapper dlqMapper;
    private final CircuitBreakerRegistry cbRegistry;
    private final CanvasMetrics metrics;
    private final ObjectMapper objectMapper;
    private final ContextPersistenceService ctxStore;
    /** @Lazy 避免与 CanvasExecutionService → DagEngine 的循环依赖 */
    private final org.chovy.canvas.engine.trigger.CanvasExecutionService executionService;

    public DagEngine(HandlerRegistry handlerRegistry,
                     CanvasExecutionTraceMapper traceMapper,
                     TraceWriteBuffer traceBuffer,
                     CanvasExecutionDlqMapper dlqMapper,
                     CircuitBreakerRegistry cbRegistry,
                     CanvasMetrics metrics,
                     ObjectMapper objectMapper,
                     ContextPersistenceService ctxStore,
                     @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService) {
        this.handlerRegistry  = handlerRegistry;
        this.traceMapper      = traceMapper;
        this.traceBuffer      = traceBuffer;
        this.dlqMapper        = dlqMapper;
        this.cbRegistry       = cbRegistry;
        this.metrics          = metrics;
        this.objectMapper     = objectMapper;
        this.ctxStore         = ctxStore;
        this.executionService = executionService;
    }

    @Value("${canvas.execution.max-retry:3}")
    private int maxRetry;

    @Value("${canvas.execution.retry-base-delay-ms:1000}")
    private long retryBaseDelayMs;

    @Value("${canvas.execution.retry-max-delay-ms:30000}")
    private long retryMaxDelayMs;

    /**
     * 虚拟线程调度器，供阻塞型 Handler 使用
     */
    private static final reactor.core.scheduler.Scheduler VIRTUAL =
            Schedulers.fromExecutorService(Executors.newVirtualThreadPerTaskExecutor());

    // ══════════════════════════════════════════════════════════════
    // 公开入口
    // ══════════════════════════════════════════════════════════════

    /**
     * 从触发器节点开始执行整个画布 DAG。
     * 执行完成后（无论成功/失败）批量写入所有未执行节点的 SKIPPED 轨迹。
     */
    public Mono<Map<String, Object>> execute(DagGraph graph, String triggerNodeId,
                                             ExecutionContext ctx) {
        return executeNode(graph, triggerNodeId, ctx, 0)
                .doFinally(__ -> writeSkippedNodes(graph, ctx))
                .onErrorResume(e -> {
                    log.error("[ENGINE] 执行出错 executionId={}: {}",
                            ctx.getExecutionId(), e.getMessage(), e);
                    return Mono.just(Map.of("error", e.getMessage()));
                });
    }

    /**
     * 最大 DAG 递归深度（防止超深链路或隐式循环导致 StackOverflowError）
     */
    private static final int MAX_NODE_DEPTH = 200;

    // ══════════════════════════════════════════════════════════════
    // 单节点执行（6 阶段，严格遵循 7.4 节）
    // ══════════════════════════════════════════════════════════════

    private Mono<Map<String, Object>> executeNode(DagGraph graph, String nodeId,
                                                  ExecutionContext ctx) {
        return executeNode(graph, nodeId, ctx, 0);
    }

    private Mono<Map<String, Object>> executeNode(DagGraph graph, String nodeId,
                                                  ExecutionContext ctx, int depth) {
        if (depth > MAX_NODE_DEPTH) {
            return Mono.error(new IllegalStateException(
                    "[ENGINE] DAG 执行深度超限（" + MAX_NODE_DEPTH + "），" +
                            "可能存在隐式循环或超大画布 nodeId=" + nodeId));
        }

        DagParser.CanvasNode node = graph.getNode(nodeId);
        if (node == null) {
            log.warn("[ENGINE] 节点不存在，跳过 nodeId={}", nodeId);
            return Mono.just(Map.of());
        }

        return Mono.defer(() -> {

            // ──────────────────────────────────────────────────────
            // 阶段 1：解析节点配置（CONTEXT 类型替换为实际值）
            // bizConfig 兜底（触发器等节点 nextNodeId 只存在 bizConfig），config 覆盖
            Map<String, Object> rawConfig = new HashMap<>();
            if (node.getBizConfig() != null) rawConfig.putAll(node.getBizConfig());
            if (node.getConfig() != null) rawConfig.putAll(node.getConfig());

            Map<String, Object> config = NodeType.MANUAL_APPROVAL.equals(node.getType())
                    ? resolveConfigWithNodeId(rawConfig, ctx, nodeId)
                    : resolveConfig(rawConfig, ctx);

            // ──────────────────────────────────────────────────────
            // 阶段 2：LOGIC_RELATION / HUB 特殊处理
            // ──────────────────────────────────────────────────────
            if (NodeType.LOGIC_RELATION.equals(node.getType())) {
                return handleLogicRelation(graph, nodeId, node, config, ctx);
            }
            if (NodeType.HUB.equals(node.getType())) {
                return handleHub(graph, nodeId, node, config, ctx);
            }

            // ──────────────────────────────────────────────────────
            // 阶段 3：幂等检查（已执行过则跳过）
            // ──────────────────────────────────────────────────────
            if (ctx.isNodeDone(nodeId)) {
                log.debug("[ENGINE] 幂等跳过 nodeId={}", nodeId);
                return Mono.just(Map.of());
            }

            // ──────────────────────────────────────────────────────
            // 阶段 4：CAS 抢占本地锁（waitProcess 机制）
            // ──────────────────────────────────────────────────────
            AtomicBoolean waitProcess = ctx.getLock(nodeId);
            if (!waitProcess.compareAndSet(true, false)) {
                // 抢锁失败：设置 waitProcess=true 通知持锁协程需要 repeat
                waitProcess.set(true);
                log.debug("[ENGINE] CAS 失败，设 waitProcess=true nodeId={}", nodeId);
                return Mono.just(Map.of());
            }

            // ──────────────────────────────────────────────────────
            // 阶段 5 + 6：执行 Handler（含 repeat 机制）
            // repeat 必须在写 SUCCESS / 触发下游之前检查，
            // 否则幂等检查（阶段3）会拦截 repeat 调用
            // ──────────────────────────────────────────────────────
            writeTraceStart(ctx, node);
            NodeHandler handler = handlerRegistry.get(node.getType());
            long nodeStartMs = System.currentTimeMillis(); // 记录节点开始时间

            return executeHandlerWithRepeat(handler, config, ctx, waitProcess,
                    nodeId, node.getType())
                    .flatMap(result -> {

                        if (!result.success()) {
                            // Handler 返回失败
                            waitProcess.set(true); // 释放锁
                            ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                            writeTraceEnd(ctx, node, result);
                            // 防资损：已发券/已触达则整体 SUCCESS
                            if (ctx.isBenefitGranted() || ctx.isUserReached()) {
                                log.warn("[ENGINE] 防资损：节点失败但整体判定成功 nodeId={}", nodeId);
                                return Mono.just(new HashMap<String, Object>());
                            }
                            return Mono.error(
                                    new RuntimeException("节点 " + nodeId + " 失败: " + result.errorMessage()));
                        }

                        // ── 阶段 6：写输出，设状态，触发下游 ──────────
                        if (handler.isBenefitNode()) ctx.setBenefitGranted(true);
                        if (handler.isReachNode()) ctx.setUserReached(true);
                        if (result.output() != null && !result.output().isEmpty()) {
                            ctx.putNodeOutput(nodeId, result.output());
                        }

                        ctx.setNodeStatus(nodeId, NodeStatus.SUCCESS);
                        long durationMs = System.currentTimeMillis() - nodeStartMs;
                        writeTraceEnd(ctx, node, result, durationMs);
                        metrics.recordNodeExecution(node.getType(), NodeStatus.SUCCESS.name(), durationMs);
                        log.debug("[ENGINE] 节点完成 nodeId={} type={}", nodeId, node.getType());

                        return triggerDownstream(graph, result, nodeId, node.getType(), ctx);
                    })
                    .onErrorResume(e -> {
                        waitProcess.set(true);
                        ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                        log.error("[ENGINE] 节点异常 nodeId={}: {}", nodeId, e.getMessage());
                        if (ctx.isBenefitGranted() || ctx.isUserReached()) {
                            return Mono.just(new HashMap<>());
                        }
                        return Mono.error(e);
                    });
        });
    }

    // ══════════════════════════════════════════════════════════════
    // ══════════════════════════════════════════════════════════════
    // repeat 并发保护 + 熔断 + 重试 + DLQ（7.5节 + 12.5节 + 13.2节 + 13.3节）
    // ══════════════════════════════════════════════════════════════

    private Mono<NodeResult> executeHandlerWithRepeat(NodeHandler handler,
                                                      Map<String, Object> config,
                                                      ExecutionContext ctx,
                                                      AtomicBoolean waitProcess,
                                                      String nodeId,
                                                      String nodeType) {
        CircuitBreakerRegistry.CircuitBreaker cb = cbRegistry.get(nodeType);

        // 单次调用：直接调用 Handler 的响应式方法，无需 subscribeOn（Handler 自行决定调度）
        // 熔断检查用 Mono.defer() 包裹，确保每次订阅时才检查熔断状态
        Mono<NodeResult> singleCall = Mono.defer(() -> {
                    try {
                        cb.checkState(); // OPEN 时抛 CircuitBreakerOpenException（不可重试）
                    } catch (CircuitBreakerRegistry.CircuitBreakerOpenException e) {
                        return Mono.just(NodeResult.fail(e.getMessage()));
                    }
                    return handler.executeAsync(config, ctx);
                })
                .doOnNext(r -> {
                    if (r.success()) cb.recordSuccess();
                    else cb.recordFailure();
                })
                .doOnError(e -> cb.recordFailure());

        // 指数退避重试（13.2节）：只重试可重试异常
        Mono<NodeResult> withRetry = singleCall
                .retryWhen(Retry.backoff(maxRetry, Duration.ofMillis(retryBaseDelayMs))
                        .maxBackoff(Duration.ofMillis(retryMaxDelayMs))
                        .filter(this::isRetryable)
                        .doBeforeRetry(sig -> {
                            metrics.recordNodeRetry(nodeType);
                            log.warn("[ENGINE] 节点重试 nodeId={} attempt={} reason={}",
                                    nodeId, sig.totalRetries() + 1, sig.failure().getMessage());
                        }))
                // 重试耗尽或不可重试异常 → 写 DLQ（13.3节），返回 FAILED
                .onErrorResume(e -> {
                    writeDlq(ctx, nodeId, nodeType, e);
                    return Mono.just(NodeResult.fail("已写入DLQ: " + e.getMessage()));
                });

        // repeat 并发保护（7.5节）：在写 SUCCESS 之前检查
        return withRetry.flatMap(result -> {
            if (!result.success()) {
                return Mono.just(result); // 失败直接返回，调用方释放锁
            }

            // getAndSet(true)：释放锁，返回旧值。旧值=true → 有协程在等待 → repeat
            boolean hadWaiter = waitProcess.getAndSet(true);
            if (hadWaiter && waitProcess.compareAndSet(true, false)) {
                log.debug("[ENGINE] repeat nodeId={}", nodeId);
                return withRetry.doFinally(__ -> waitProcess.set(true));
            }

            return Mono.just(result);
        });
    }

    /**
     * 可重试异常（13.2节白名单）
     */
    private boolean isRetryable(Throwable ex) {
        if (ex instanceof CircuitBreakerRegistry.CircuitBreakerOpenException) return false;
        if (ex instanceof java.net.SocketTimeoutException) return true;
        if (ex instanceof java.net.ConnectException) return true;
        String msg = ex.getMessage();
        return msg != null && (msg.contains("5xx") || msg.contains("timeout") || msg.contains("Timeout"));
    }

    /**
     * 写入死信队列（13.3节）
     */
    private void writeDlq(ExecutionContext ctx, String nodeId, String nodeType, Throwable cause) {
        metrics.recordDlq(nodeType);
        try {
            String msg = cause.getMessage() != null ? cause.getMessage() : "unknown";
            CanvasExecutionDlq dlq = CanvasExecutionDlq.builder()
                    .executionId(ctx.getExecutionId())
                    .canvasId(ctx.getCanvasId())
                    .userId(ctx.getUserId())
                    .failedNodeId(nodeId)
                    .failedNodeType(nodeType)
                    .errorMsg(msg.substring(0, Math.min(500, msg.length())))
                    .retryCount(maxRetry)
                    .triggerPayload(objectMapper.writeValueAsString(ctx.getTriggerPayload()))
                    // 保存原始触发信息，供 DLQ 重放使用（修复：replay 不再写死 DIRECT_CALL）
                    .triggerType(ctx.getTriggerType())
                    .triggerNodeType(ctx.getTriggerNodeType())
                    .matchKey(ctx.getMatchKey())
                    .failedAt(LocalDateTime.now())
                    .build();
            Mono.fromRunnable(() -> dlqMapper.insert(dlq))
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(null, e -> log.error("[DLQ] 写入失败: {}", e.getMessage()));
            log.warn("[DLQ] executionId={} nodeId={} reason={}", ctx.getExecutionId(), nodeId, msg);
        } catch (Exception e) {
            log.error("[DLQ] 序列化失败: {}", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // LOGIC_RELATION 处理（设计文档 7.4 阶段 2 + 7.7 节边界行为）
    // ══════════════════════════════════════════════════════════════

    private Mono<Map<String, Object>> handleLogicRelation(DagGraph graph, String nodeId,
                                                          DagParser.CanvasNode node,
                                                          Map<String, Object> config,
                                                          ExecutionContext ctx) {
        List<String> upstreamIds = graph.upstream(nodeId);
        String relation = (String) config.getOrDefault("relation", "AND");

        // AND 模式：上游有 FAILED/SKIPPED → 立即失败（7.7节）
        if (LogicRelationHandler.shouldFailImmediately(relation, upstreamIds, ctx)) {
            ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
            log.warn("[ENGINE] LOGIC_RELATION AND 上游失败，立即 FAILED nodeId={}", nodeId);
            if (ctx.isBenefitGranted() || ctx.isUserReached()) return Mono.just(Map.of());
            return Mono.error(new RuntimeException("LOGIC_RELATION AND 条件因上游失败不可满足"));
        }

        // 条件未满足：进入等待态，设置 WAITING 状态（供 isPaused 检测）
        if (!LogicRelationHandler.checkCondition(relation, upstreamIds, ctx)) {
            ctx.setNodeStatus(nodeId, NodeStatus.WAITING);

            // LOGIC_RELATION 等待超时（与 Hub 类似，防止第二触发永不到来）
            // config 中可选配置 timeout 字段（秒），默认等于全局执行超时
            if (!ctx.getScheduledHubTimeouts().contains("lr:" + nodeId)) {
                ctx.getScheduledHubTimeouts().add("lr:" + nodeId);
                ctx.getHubStartTimes().putIfAbsent("lr:" + nodeId, System.currentTimeMillis());
                int timeoutSec = config.get("timeout") instanceof Number n
                        ? n.intValue() : (int) globalTimeout;

                Mono.delay(Duration.ofSeconds(timeoutSec), VIRTUAL)
                        .subscribe(__ -> {
                            if (ctx.getNodeStatus(nodeId) == NodeStatus.WAITING) {
                                log.warn("[ENGINE] LOGIC_RELATION 等待超时 timeout={}s nodeId={}", timeoutSec, nodeId);
                                ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                                ctxStore.save(ctx);
                                executionService.trigger(
                                        ctx.getCanvasId(), ctx.getUserId(),
                                        TriggerType.LOGIC_RELATION_TIMEOUT, NodeType.LOGIC_RELATION,
                                        null, Map.of(),
                                        ctx.getExecutionId() + ":lr-timeout:" + nodeId, false)
                                        .subscribe(null,
                                                e -> log.error("[LOGIC_RELATION] 超时恢复失败 nodeId={}: {}", nodeId, e.getMessage()));
                            }
                        });
                log.debug("[LOGIC_RELATION] 启动等待超时定时器 {}s nodeId={}", timeoutSec, nodeId);
            }
            log.debug("[ENGINE] LOGIC_RELATION 条件未满足，进入 WAITING nodeId={}", nodeId);
            return Mono.just(Map.of());
        }

        // 条件满足 → 走正常节点执行流程（阶段 3-6）
        return executeNodeAfterStage2(graph, nodeId, node, config, ctx);
    }

    @org.springframework.beans.factory.annotation.Value("${canvas.execution.global-timeout-sec:600}")
    private long globalTimeout;

    // ══════════════════════════════════════════════════════════════
    // HUB 处理（含超时延迟任务，设计文档 HUB 节点说明）
    // ══════════════════════════════════════════════════════════════

    /**
     *核心机制：每个上游节点完成时，主动调用 executeNode(HUB)
     * <br/>
     *   上游A完成 → executeNode(HUB) → allUpstreamDone=false → WAITING → 返回空Map
     *   上游B完成 → executeNode(HUB) → allUpstreamDone=true  → 继续执行
     * <br/>
     *   触发重入的是 DagEngine 的 CAS repeat 机制：
     *   核心机制：每个上游节点完成时，主动调用 executeNode(HUB)
     * <br/>
     *   上游A完成 → executeNode(HUB) → allUpstreamDone=false → WAITING → 返回空Map
     *   上游B完成 → executeNode(HUB) → allUpstreamDone=true  → 继续执行
     * <br/>
     *   触发重入的是 DagEngine 的 CAS repeat 机制:
     *   完整流程是这样的：
     * <br/>
     *   上游A完成
     *     └> executeNode(HUB)
     *          ├─ 抢到 CAS 锁
     *          ├─ allUpstreamDone=false → WAITING → 返回 Map.of()
     *          └─ 检查 waitProcess → false → 结束
     *                               上游B完成
     *                                 └> executeNode(HUB)
     *                                      ├─ 抢锁失败（A还没释放）→ set waitProcess=true → 返回
     *                                      或者
     *                                      ├─ 抢锁成功（A已释放）
     *                                      ├─ allUpstreamDone=true → 继续执行下游
     *                                      └─ 正常结束
     * <br/>
     *   若抢锁失败路径：A 在释放锁前发现 waitProcess=true
     *     └> repeat：A 重新执行 handleHub → allUpstreamDone=true → 继续
     * <br/>
     *   Mono.delay() 是超时兜底，不是主驱动，它只做一件事：N 秒后如果 Hub 还没完成就标记 FAILED。
     * <br/>
     *
     */
    private Mono<Map<String, Object>> handleHub(DagGraph graph, String nodeId,
                                                DagParser.CanvasNode node,
                                                Map<String, Object> config,
                                                ExecutionContext ctx) {
        List<String> upstreamIds = graph.upstream(nodeId);

        // 所有上游未完成：进入等待态
        if (!HubHandler.allUpstreamDone(upstreamIds, ctx)) {
            ctx.setNodeStatus(nodeId, NodeStatus.WAITING);  // 设 WAITING 供 isPaused 检测
            if (!ctx.getScheduledHubTimeouts().contains(nodeId)) {
                ctx.getScheduledHubTimeouts().add(nodeId);
                ctx.getHubStartTimes().putIfAbsent(nodeId, System.currentTimeMillis());
                int timeoutSec = HubHandler.getTimeoutSeconds(config);

                // 延迟任务：超时后若 Hub 仍未完成则标记 FAILED，持久化 ctx，触发执行恢复
                Mono.delay(Duration.ofSeconds(timeoutSec), VIRTUAL)
                        .subscribe(__ -> {
                            if (!ctx.isNodeDone(nodeId)) {
                                log.warn("[HUB] 等待超时 timeout={}s nodeId={}", timeoutSec, nodeId);
                                ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                                ctxStore.save(ctx);   // 同步回写 Redis，避免内存与持久化状态不一致
                                // 触发执行恢复：让执行引擎感知 FAILED 状态并继续后续处理
                                executionService.trigger(
                                        ctx.getCanvasId(), ctx.getUserId(),
                                        TriggerType.HUB_TIMEOUT, NodeType.HUB,
                                        null, Map.of(),
                                        ctx.getExecutionId() + ":hub-timeout:" + nodeId, false)
                                        .subscribe(null,
                                                e -> log.error("[HUB] 超时恢复失败 nodeId={}: {}", nodeId, e.getMessage()));
                            }
                        });

                log.debug("[HUB] 启动超时定时器 {}s nodeId={}", timeoutSec, nodeId);
            } else {
                // 已调度过定时器，检查是否已超时
                long start = ctx.getHubStartTimes().getOrDefault(nodeId, System.currentTimeMillis());
                int timeout = HubHandler.getTimeoutSeconds(config);
                if (System.currentTimeMillis() - start > (long) timeout * 1000) {
                    ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                    return Mono.error(new RuntimeException("HUB 等待超时 nodeId=" + nodeId));
                }
            }
            return Mono.just(Map.of()); // 继续等待
        }

        // 所有上游完成 → 走正常节点执行流程（阶段 3-6）
        return executeNodeAfterStage2(graph, nodeId, node, config, ctx);
    }

    // ══════════════════════════════════════════════════════════════
    // 阶段 3-6 公共入口（LOGIC_RELATION/HUB 满足条件后调用）
    // ══════════════════════════════════════════════════════════════

    private Mono<Map<String, Object>> executeNodeAfterStage2(DagGraph graph, String nodeId,
                                                             DagParser.CanvasNode node,
                                                             Map<String, Object> config,
                                                             ExecutionContext ctx) {
        // 阶段 3：幂等
        if (ctx.isNodeDone(nodeId)) return Mono.just(Map.of());

        // 阶段 4：CAS
        AtomicBoolean waitProcess = ctx.getLock(nodeId);
        if (!waitProcess.compareAndSet(true, false)) {
            waitProcess.set(true);
            return Mono.just(Map.of());
        }

        writeTraceStart(ctx, node);
        NodeHandler handler = handlerRegistry.get(node.getType());

        return executeHandlerWithRepeat(handler, config, ctx, waitProcess,
                nodeId, node.getType())
                .flatMap(result -> {
                    if (!result.success()) {
                        waitProcess.set(true);
                        ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                        writeTraceEnd(ctx, node, result);
                        if (ctx.isBenefitGranted() || ctx.isUserReached()) return Mono.just(Map.<String, Object>of());
                        return Mono.error(new RuntimeException("节点 " + nodeId + " 失败: " + result.errorMessage()));
                    }
                    if (handler.isBenefitNode()) ctx.setBenefitGranted(true);
                    if (handler.isReachNode()) ctx.setUserReached(true);
                    if (result.output() != null && !result.output().isEmpty()) {
                        ctx.putNodeOutput(nodeId, result.output());
                    }
                    ctx.setNodeStatus(nodeId, NodeStatus.SUCCESS);
                    writeTraceEnd(ctx, node, result);
                    return triggerDownstream(graph, result, nodeId, node.getType(), ctx);
                })
                .onErrorResume(e -> {
                    waitProcess.set(true);
                    ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                    if (ctx.isBenefitGranted() || ctx.isUserReached()) {
                        return Mono.just(new HashMap<>());
                    }
                    return Mono.error(e);
                });
    }

    // ══════════════════════════════════════════════════════════════
    // 触发下游节点（含 Priority 串行逻辑，设计文档 4.6 节）
    // ══════════════════════════════════════════════════════════════

    private Mono<Map<String, Object>> triggerDownstream(DagGraph graph, NodeResult result,
                                                        String sourceNodeId, String sourceType,
                                                        ExecutionContext ctx) {
        // 立即标记未走的分支入口为 SKIPPED（设计文档 7.7节两阶段写入 - 阶段一）
        // 目的：让 LOGIC_RELATION(AND) 在执行中即时检测到上游 SKIPPED，而不是等到执行结束
        markNonTakenBranchesSkipped(graph, sourceNodeId, sourceType, result, ctx);

        // PRIORITY 节点：串行依序尝试，第一个成功则停止（4.6节）
        if (NodeType.PRIORITY.equals(sourceType) && result.branchMap() != null) {
            List<String> orderedBranches = result.branchMap().values().stream()
                    .filter(Objects::nonNull).toList();
            String fallbackNextId = result.elseNodeId();
            return tryPrioritySequentially(orderedBranches, fallbackNextId, sourceNodeId,
                    graph, ctx);
        }

        // 普通节点：收集所有下游并行触发
        List<String> nextIds = collectNextIds(result);
        if (nextIds.isEmpty()) {
            return Mono.just(result.output() != null ? result.output() : Map.of());
        }

        return Flux.fromIterable(nextIds)
                .flatMap(nextId -> executeNode(graph, nextId, ctx))
                .last(Map.of())
                .defaultIfEmpty(Map.of());
    }

    /**
     * Priority 串行执行：按顺序尝试每个分支，成功则停止，失败则尝试下一个。
     * 全部失败时若有 fallback(nextNodeId) 则走 fallback（设 PARTIAL_FAIL），否则整体 FAILED。
     */
    private Mono<Map<String, Object>> tryPrioritySequentially(List<String> branches,
                                                              String fallbackNextId,
                                                              String priorityNodeId,
                                                              DagGraph graph,
                                                              ExecutionContext ctx) {
        if (branches.isEmpty()) {
            // 所有分支均失败
            if (fallbackNextId != null) {
                ctx.setNodeStatus(priorityNodeId, NodeStatus.PARTIAL_FAIL);
                log.debug("[PRIORITY] 所有分支失败，走 fallback nextId={}", fallbackNextId);
                return executeNode(graph, fallbackNextId, ctx);
            }
            log.debug("[PRIORITY] 所有分支失败，无 fallback，整体 FAILED");
            return Mono.error(new RuntimeException("PRIORITY 所有分支均失败"));
        }

        String currentBranchId = branches.get(0);
        return executeNode(graph, currentBranchId, ctx)
                .flatMap(__ -> {
                    if (ctx.getNodeStatus(currentBranchId) == NodeStatus.SUCCESS) {
                        log.debug("[PRIORITY] 分支成功，停止 branchId={}", currentBranchId);
                        return Mono.just(Map.of());
                    }
                    // 当前分支失败，尝试下一个
                    log.debug("[PRIORITY] 分支失败，尝试下一个 branchId={}", currentBranchId);
                    return tryPrioritySequentially(
                            branches.subList(1, branches.size()),
                            fallbackNextId, priorityNodeId, graph, ctx);
                });
    }

    // ══════════════════════════════════════════════════════════════
    // 执行结束批量写入 SKIPPED（设计文档 7.7 节）
    //
    // 在执行完成后，扫描 DAG 中所有节点，对从未进入 nodeStatuses 的节点
    // 批量写入 SKIPPED 轨迹记录，无需节点执行逻辑主动写入。
    // ══════════════════════════════════════════════════════════════

    private void writeSkippedNodes(DagGraph graph, ExecutionContext ctx) {
        List<CanvasExecutionTrace> skippedTraces = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        graph.getNodeMap().forEach((nodeId, node) -> {
            if (!ctx.getNodeStatuses().containsKey(nodeId)) {
                ctx.setNodeStatus(nodeId, NodeStatus.SKIPPED);
                skippedTraces.add(CanvasExecutionTrace.builder()
                        .executionId(ctx.getExecutionId())
                        .nodeId(nodeId)
                        .nodeType(node.getType())
                        .nodeName(node.getName())
                        .status(3) // SKIPPED
                        .startedAt(now)
                        .finishedAt(now)
                        .build());
            }
        });

        if (!skippedTraces.isEmpty()) {
            // 批量异步写入（TraceWriteBuffer 批量刷盘，不阻塞主执行链路）
            skippedTraces.forEach(traceBuffer::offer);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // ══════════════════════════════════════════════════════════════
    // 执行轨迹写入（通过 TraceWriteBuffer 异步批量写，12.10节）
    // ══════════════════════════════════════════════════════════════

    private void writeTraceStart(ExecutionContext ctx, DagParser.CanvasNode node) {
        CanvasExecutionTrace trace = CanvasExecutionTrace.builder()
                .executionId(ctx.getExecutionId())
                .nodeId(node.getId())
                .nodeType(node.getType())
                .nodeName(node.getName())
                .status(0) // 执行中
                .startedAt(LocalDateTime.now())
                .build();
        traceBuffer.offer(trace); // 非阻塞入队
    }

    /**
     * 重载：带 durationMs（节点实际耗时）
     */
    private void writeTraceEnd(ExecutionContext ctx, DagParser.CanvasNode node,
                               NodeResult result, long durationMs) {
        int status = result.success() ? 1 : 2;
        String outputJson = null;
        try {
            if (result.output() != null && !result.output().isEmpty()) {
                outputJson = objectMapper.writeValueAsString(result.output());
            }
        } catch (Exception ignored) {
        }

        CanvasExecutionTrace trace = CanvasExecutionTrace.builder()
                .executionId(ctx.getExecutionId())
                .nodeId(node.getId())
                .nodeType(node.getType())
                .nodeName(node.getName())
                .status(status)
                .outputData(outputJson)
                .errorMsg(result.errorMessage())
                .finishedAt(LocalDateTime.now())
                .durationMs(durationMs > 0 ? durationMs : null)
                .build();
        traceBuffer.offer(trace);
    }

    private void writeTraceEnd(ExecutionContext ctx, DagParser.CanvasNode node, NodeResult result) {
        writeTraceEnd(ctx, node, result, 0);
    }

    // ══════════════════════════════════════════════════════════════
    // 工具方法
    // ══════════════════════════════════════════════════════════════

    /**
     * 从 NodeResult 收集所有下游节点 ID（排除 null）
     */
    private List<String> collectNextIds(NodeResult result) {
        List<String> ids = new ArrayList<>();
        if (result.nextNodeId() != null) ids.add(result.nextNodeId());
        if (result.successNodeId() != null) ids.add(result.successNodeId());
        if (result.failNodeId() != null) ids.add(result.failNodeId());
        if (result.elseNodeId() != null) ids.add(result.elseNodeId());
        if (result.branchMap() != null) ids.addAll(result.branchMap().values());
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    /**
     * 解析节点配置：将 valueType=CONTEXT 的字段替换为上下文实际值。
     * 设计文档 7.4 阶段 1。
     * 例如:
     * {
     *   "apiKey": "send_sms",
     *   "params": {
     *     "phone": {
     *       "valueType": "CONTEXT",
     *       "value": "user.phone"  // ← 从上下文读取用户手机号
     *     },
     *     "content": {
     *       "valueType": "CONTEXT",
     *       "value": "trigger.message"  // ← 从触发事件读取消息内容
     *     }
     *   }
     * }
     * 会被处理为:
     * {
     *   "apiKey": "send_sms",
     *   "params": {
     *     "phone": "13800138000",       // ← 实际手机号
     *     "content": "您的订单已发货"    // ← 实际消息
     *   }
     * }
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveConfig(Map<String, Object> config, ExecutionContext ctx) {
        if (config == null) return Map.of();

        // 性能优化：先检查是否有任何 CONTEXT 字段，无则直接返回原 Map 避免 HashMap 创建
        boolean hasContextField = config.values().stream().anyMatch(
                v -> v instanceof Map<?, ?> m && "CONTEXT".equals(m.get("valueType")));
        if (!hasContextField) return config;   // O(n) 扫描，但避免了 HashMap allocation

        Map<String, Object> resolved = new HashMap<>(config);
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> m) {
                String valueType = (String) ((Map<String, Object>) m).get("valueType");
                String value = (String) ((Map<String, Object>) m).get("value");
                if ("CONTEXT".equals(valueType) && value != null) {
                    resolved.put(entry.getKey(), ctx.getContextValue(value));
                }
            }
        }
        return resolved;
    }

    /**
     * nodeId 注入版（ManualApprovalHandler 需要知道自身 nodeId）
     */
    private Map<String, Object> resolveConfigWithNodeId(Map<String, Object> config,
                                                        ExecutionContext ctx, String nodeId) {
        Map<String, Object> resolved = new HashMap<>(resolveConfig(config, ctx));
        resolved.put("__nodeId", nodeId);
        return resolved;
    }

    /**
     * 立即标记未走分支的入口节点为 SKIPPED（设计文档 7.7节 阶段一写入）。
     * 必要性：LOGIC_RELATION(AND) 在执行中检查上游 SKIPPED，
     * 若 SKIPPED 只在执行结束后才写，则 LOGIC_RELATION 永远看到 PENDING，永久等待。
     * 覆盖场景：
     * IF_CONDITION  → 标记未走的 success/fail 分支入口
     * SELECTOR      → 标记未命中的所有 branch 入口（含 else）
     */
    @SuppressWarnings("unchecked")
    private void markNonTakenBranchesSkipped(DagGraph graph, String sourceNodeId,
                                             String sourceType, NodeResult result,
                                             ExecutionContext ctx) {
        DagParser.CanvasNode node = graph.getNode(sourceNodeId);
        if (node == null || node.getConfig() == null) return;
        Map<String, Object> cfg = node.getConfig();

        if (NodeType.IF_CONDITION.equals(sourceType)) {
            // result 中只有被走的那条（另一条是 null）
            // 通过 config 找到"另一条"并标记 SKIPPED
            String successId = (String) cfg.get("successNodeId");
            String failId = (String) cfg.get("failNodeId");
            String takenId = result.successNodeId() != null ? result.successNodeId()
                    : result.failNodeId();
            String skippedId = takenId != null && takenId.equals(successId) ? failId : successId;
            markSkipped(skippedId, ctx);

        } else if ("SELECTOR".equals(sourceType)) {
            // 标记所有未走的 branch 入口
            java.util.List<Map<String, Object>> branches =
                    (java.util.List<Map<String, Object>>) cfg.getOrDefault("branches", List.of());
            String elseId = (String) cfg.get("elseNodeId");
            String takenId = result.nextNodeId(); // SELECTOR 走的那条

            branches.forEach(b -> {
                String branchNext = (String) b.get("nextNodeId");
                if (branchNext != null && !branchNext.equals(takenId)) {
                    markSkipped(branchNext, ctx);
                }
            });
            if (elseId != null && !elseId.equals(takenId)) {
                markSkipped(elseId, ctx);
            }
        }
    }

    private void markSkipped(String nodeId, ExecutionContext ctx) {
        if (nodeId != null && !ctx.isNodeDone(nodeId)) {
            ctx.setNodeStatus(nodeId, NodeStatus.SKIPPED);
            log.debug("[ENGINE] 立即标记 SKIPPED nodeId={}", nodeId);
        }
    }
}
