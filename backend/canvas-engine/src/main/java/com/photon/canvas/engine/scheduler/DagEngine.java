package com.photon.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.photon.canvas.domain.execution.CanvasExecutionTrace;
import com.photon.canvas.domain.execution.CanvasExecutionTraceMapper;
import com.photon.canvas.engine.context.ExecutionContext;
import com.photon.canvas.engine.context.NodeStatus;
import com.photon.canvas.engine.dag.DagGraph;
import com.photon.canvas.engine.dag.DagParser;
import com.photon.canvas.engine.handler.HandlerRegistry;
import com.photon.canvas.engine.handler.NodeHandler;
import com.photon.canvas.engine.handler.NodeResult;
import com.photon.canvas.engine.handlers.HubHandler;
import com.photon.canvas.engine.handlers.LogicRelationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
    private final CanvasExecutionTraceMapper traceMapper;
    private final ObjectMapper               objectMapper;

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
        return executeNode(graph, triggerNodeId, ctx)
                .doFinally(__ -> writeSkippedNodes(graph, ctx))
                .onErrorResume(e -> {
                    log.error("[ENGINE] 执行出错 executionId={}: {}",
                            ctx.getExecutionId(), e.getMessage(), e);
                    return Mono.just(Map.of("error", e.getMessage()));
                });
    }

    // ══════════════════════════════════════════════════════════════
    // 单节点执行（6 阶段，严格遵循 7.4 节）
    // ══════════════════════════════════════════════════════════════

    private Mono<Map<String, Object>> executeNode(DagGraph graph, String nodeId,
                                                    ExecutionContext ctx) {
        DagParser.CanvasNode node = graph.getNode(nodeId);
        if (node == null) return Mono.just(Map.of());

        return Mono.defer(() -> {

            // ──────────────────────────────────────────────────────
            // 阶段 1：解析节点配置（CONTEXT 类型替换为实际值）
            // ──────────────────────────────────────────────────────
            Map<String, Object> config = resolveConfig(node.getConfig(), ctx);

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

            return executeHandlerWithRepeat(handler, config, ctx, waitProcess)
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
    // repeat 并发保护（设计文档 7.5 节）
    //
    // 机制说明：
    //   1. CAS(true→false) 抢锁（阶段 4 已完成，进入此方法时已持锁）
    //   2. 执行 Handler
    //   3. getAndSet(true)：释放锁并返回旧值
    //      - 旧值=true  ↔ 有协程在等待时设置了 waitProcess=true → needRepeat
    //      - 旧值=false ↔ 无协程等待 → 无需 repeat
    //   4. 若 needRepeat：重新 CAS 抢锁并再执行一次（最多一次）
    //
    // 关键：repeat 检查在写 SUCCESS 状态之前完成，确保 repeat 不被幂等拦截
    // ══════════════════════════════════════════════════════════════

    private Mono<NodeResult> executeHandlerWithRepeat(NodeHandler handler,
                                                        Map<String, Object> config,
                                                        ExecutionContext ctx,
                                                        AtomicBoolean waitProcess) {
        return Mono.fromCallable(() -> handler.execute(config, ctx))
                .subscribeOn(VIRTUAL)
                .flatMap(result -> {
                    if (!result.success()) {
                        return Mono.just(result); // 失败直接返回，由调用方释放锁
                    }

                    // getAndSet(true)：释放锁，同时检查是否有协程在等待
                    boolean hadWaiter = waitProcess.getAndSet(true);

                    if (hadWaiter) {
                        // 有协程等待，重新 CAS 抢锁执行一次
                        if (waitProcess.compareAndSet(true, false)) {
                            log.debug("[ENGINE] repeat 触发（有并发协程等待）");
                            return Mono.fromCallable(() -> handler.execute(config, ctx))
                                    .subscribeOn(VIRTUAL)
                                    .doFinally(__ -> waitProcess.set(true)); // repeat 结束后释放锁
                        }
                        // CAS 失败：另一协程已抢先处理，返回当前结果
                    }

                    // 无等待或未能 re-acquire：直接返回（锁已释放）
                    return Mono.just(result);
                });
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

        // 条件未满足：等待其他上游
        if (!LogicRelationHandler.checkCondition(relation, upstreamIds, ctx)) {
            log.debug("[ENGINE] LOGIC_RELATION 条件未满足，等待上游 nodeId={}", nodeId);
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
            // 首次进入等待时启动超时定时器
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

        return executeHandlerWithRepeat(handler, config, ctx, waitProcess)
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
    // 执行轨迹写入（异步，不阻塞主链路）
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
        Mono.fromRunnable(() -> traceMapper.insert(trace))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, e -> log.warn("[TRACE] 写入开始轨迹失败: {}", e.getMessage()));
    }

    private void writeTraceEnd(ExecutionContext ctx, DagParser.CanvasNode node, NodeResult result) {
        // 更新已有记录（简化：直接 insert 一条 finished 记录）
        int status = result.success() ? 1 : 2;
        String outputJson = null;
        String errorMsg   = result.errorMessage();
        try {
            if (result.output() != null && !result.output().isEmpty()) {
                outputJson = objectMapper.writeValueAsString(result.output());
            }
        } catch (Exception ignored) {}

        final String finalOutput = outputJson;
        final String finalError  = errorMsg;
        Mono.fromRunnable(() -> {
            CanvasExecutionTrace trace = CanvasExecutionTrace.builder()
                    .executionId(ctx.getExecutionId())
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .nodeName(node.getName())
                    .status(status)
                    .outputData(finalOutput)
                    .errorMsg(finalError)
                    .finishedAt(LocalDateTime.now())
                    .build();
            traceMapper.insert(trace);
        }).subscribeOn(Schedulers.boundedElastic())
          .subscribe(null, e -> log.warn("[TRACE] 写入结束轨迹失败: {}", e.getMessage()));
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
                String valueType = (String) ((Map<String, Object>) m).get("valueType");
                String value     = (String) ((Map<String, Object>) m).get("value");
                if ("CONTEXT".equals(valueType) && value != null) {
                    resolved.put(entry.getKey(), ctx.getContextValue(value));
                }
            }
        }
        return resolved;
    }
}
