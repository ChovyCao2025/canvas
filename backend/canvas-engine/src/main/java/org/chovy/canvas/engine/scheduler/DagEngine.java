package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 *
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
@RequiredArgsConstructor
public class DagEngine {

    private final HandlerRegistry            handlerRegistry;
    private final CanvasExecutionTraceMapper traceMapper;      // 保留供 SKIPPED 批量写入
    private final TraceWriteBuffer           traceBuffer;      // 异步批量写入（12.10节）
    private final CanvasExecutionDlqMapper   dlqMapper;
    private final CircuitBreakerRegistry     cbRegistry;
    private final CanvasMetrics              metrics;
    private final ObjectMapper               objectMapper;

    @Value("${canvas.execution.max-retry:3}")
    private int maxRetry;

    @Value("${canvas.execution.retry-base-delay-ms:1000}")
    private long retryBaseDelayMs;

    @Value("${canvas.execution.retry-max-delay-ms:30000}")
    private long retryMaxDelayMs;

    /** 虚拟线程调度器，供阻塞型 Handler 使用 */
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

    /** 最大 DAG 递归深度（防止超深链路或隐式循环导致 StackOverflowError） */
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
            // MANUAL_APPROVAL 额外注入 __nodeId 供 Handler 识别自身
            Map<String, Object> config = "MANUAL_APPROVAL".equals(node.getType())
                    ? resolveConfigWithNodeId(node.getConfig(), ctx, nodeId)
                    : resolveConfig(node.getConfig(), ctx);

            // ──────────────────────────────────────────────────────
            // 阶段 2：LOGIC_RELATION / HUB 特殊处理
            // ──────────────────────────────────────────────────────
            if ("LOGIC_RELATION".equals(node.getType())) {
                return handleLogicRelation(graph, nodeId, node, config, ctx);
            }
            if ("HUB".equals(node.getType())) {
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
                                return Mono.just(Map.of());
                            }
                            return Mono.error(
                                    new RuntimeException("节点 " + nodeId + " 失败: " + result.errorMessage()));
                        }

                        // ── 阶段 6：写输出，设状态，触发下游 ──────────
                        if (handler.isBenefitNode()) ctx.setBenefitGranted(true);
                        if (handler.isReachNode())   ctx.setUserReached(true);
                        if (result.output() != null && !result.output().isEmpty()) {
                            ctx.putNodeOutput(nodeId, result.output());
                        }

                        ctx.setNodeStatus(nodeId, NodeStatus.SUCCESS);
                        writeTraceEnd(ctx, node, result);
                        metrics.recordNodeExecution(node.getType(), "SUCCESS", 0);
                        log.debug("[ENGINE] 节点完成 nodeId={} type={}", nodeId, node.getType());

                        return triggerDownstream(graph, result, nodeId, node.getType(), ctx);
                    })
                    .onErrorResume(e -> {
                        waitProcess.set(true);
                        ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                        log.error("[ENGINE] 节点异常 nodeId={}: {}", nodeId, e.getMessage());
                        if (ctx.isBenefitGranted() || ctx.isUserReached()) {
                            return Mono.just(Map.of());
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
                .doOnNext(r  -> { if (r.success()) cb.recordSuccess(); else cb.recordFailure(); })
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

    /** 可重试异常（13.2节白名单） */
    private boolean isRetryable(Throwable ex) {
        if (ex instanceof CircuitBreakerRegistry.CircuitBreakerOpenException) return false;
        if (ex instanceof java.net.SocketTimeoutException) return true;
        if (ex instanceof java.net.ConnectException) return true;
        String msg = ex.getMessage();
        return msg != null && (msg.contains("5xx") || msg.contains("timeout") || msg.contains("Timeout"));
    }

    /** 写入死信队列（13.3节） */
    private void writeDlq(ExecutionContext ctx, String nodeId, String nodeType, Throwable cause) {
        metrics.recordDlq(nodeType); // 记录 DLQ 指标
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
            log.debug("[ENGINE] LOGIC_RELATION 条件未满足，进入 WAITING nodeId={}", nodeId);
            return Mono.just(Map.of());
        }

        // 条件满足 → 走正常节点执行流程（阶段 3-6）
        return executeNodeAfterStage2(graph, nodeId, node, config, ctx);
    }

    // ══════════════════════════════════════════════════════════════
    // HUB 处理（含超时延迟任务，设计文档 HUB 节点说明）
    // ══════════════════════════════════════════════════════════════

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

                // 延迟任务：超时后若 Hub 仍未完成则标记 FAILED
                Mono.delay(Duration.ofSeconds(timeoutSec), VIRTUAL)
                        .subscribe(__ -> {
                            if (!ctx.isNodeDone(nodeId)) {
                                log.warn("[HUB] 等待超时 timeout={}s nodeId={}", timeoutSec, nodeId);
                                ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                            }
                        });

                log.debug("[HUB] 启动超时定时器 {}s nodeId={}", timeoutSec, nodeId);
            } else {
                // 已调度过定时器，检查是否已超时
                long start   = ctx.getHubStartTimes().getOrDefault(nodeId, System.currentTimeMillis());
                int  timeout = HubHandler.getTimeoutSeconds(config);
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
                        if (ctx.isBenefitGranted() || ctx.isUserReached()) return Mono.just(Map.of());
                        return Mono.error(new RuntimeException("节点 " + nodeId + " 失败: " + result.errorMessage()));
                    }
                    if (handler.isBenefitNode()) ctx.setBenefitGranted(true);
                    if (handler.isReachNode())   ctx.setUserReached(true);
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
                    if (ctx.isBenefitGranted() || ctx.isUserReached()) return Mono.just(Map.of());
                    return Mono.error(e);
                });
    }

    // ══════════════════════════════════════════════════════════════
    // 触发下游节点（含 Priority 串行逻辑，设计文档 4.6 节）
    // ══════════════════════════════════════════════════════════════

    private Mono<Map<String, Object>> triggerDownstream(DagGraph graph, NodeResult result,
                                                          String sourceNodeId, String sourceType,
                                                          ExecutionContext ctx) {
        // PRIORITY 节点：串行依序尝试，第一个成功则停止（4.6节）
        if ("PRIORITY".equals(sourceType) && result.branchMap() != null) {
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
                        return Mono.just(Map.<String, Object>of());
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
            // 异步批量写入，不阻塞主执行链路
            Mono.fromRunnable(() -> skippedTraces.forEach(traceMapper::insert))
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                            null,
                            e -> log.error("[ENGINE] 写入 SKIPPED 轨迹失败: {}", e.getMessage())
                    );
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

    private void writeTraceEnd(ExecutionContext ctx, DagParser.CanvasNode node, NodeResult result) {
        int status = result.success() ? 1 : 2;
        String outputJson = null;
        try {
            if (result.output() != null && !result.output().isEmpty()) {
                outputJson = objectMapper.writeValueAsString(result.output());
            }
        } catch (Exception ignored) {}

        CanvasExecutionTrace trace = CanvasExecutionTrace.builder()
                .executionId(ctx.getExecutionId())
                .nodeId(node.getId())
                .nodeType(node.getType())
                .nodeName(node.getName())
                .status(status)
                .outputData(outputJson)
                .errorMsg(result.errorMessage())
                .finishedAt(LocalDateTime.now())
                .build();
        traceBuffer.offer(trace); // 非阻塞入队（12.10节批量写入）
    }

    // ══════════════════════════════════════════════════════════════
    // 工具方法
    // ══════════════════════════════════════════════════════════════

    /** 从 NodeResult 收集所有下游节点 ID（排除 null） */
    private List<String> collectNextIds(NodeResult result) {
        List<String> ids = new ArrayList<>();
        if (result.nextNodeId()    != null) ids.add(result.nextNodeId());
        if (result.successNodeId() != null) ids.add(result.successNodeId());
        if (result.failNodeId()    != null) ids.add(result.failNodeId());
        if (result.elseNodeId()    != null) ids.add(result.elseNodeId());
        if (result.branchMap()     != null) ids.addAll(result.branchMap().values());
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    /**
     * 解析节点配置：将 valueType=CONTEXT 的字段替换为上下文实际值。
     * 设计文档 7.4 阶段 1。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveConfig(Map<String, Object> config, ExecutionContext ctx) {
        if (config == null) return Map.of();
        Map<String, Object> resolved = new HashMap<>(config);
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> m) {
                @SuppressWarnings("unchecked")
                String valueType = (String) ((Map<String, Object>) m).get("valueType");
                @SuppressWarnings("unchecked")
                String value     = (String) ((Map<String, Object>) m).get("value");
                if ("CONTEXT".equals(valueType) && value != null) {
                    resolved.put(entry.getKey(), ctx.getContextValue(value));
                }
            }
        }
        return resolved;
    }

    /** nodeId 注入版（ManualApprovalHandler 需要知道自身 nodeId） */
    private Map<String, Object> resolveConfigWithNodeId(Map<String, Object> config,
                                                          ExecutionContext ctx, String nodeId) {
        Map<String, Object> resolved = new HashMap<>(resolveConfig(config, ctx));
        resolved.put("__nodeId", nodeId);
        return resolved;
    }
}
