package com.photon.canvas.engine.scheduler;

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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * DAG 执行调度器（核心）。
 *
 * 调度策略：
 * 1. 从触发器节点出发，Reactor flatMap 驱动异步链。
 * 2. LOGIC_RELATION / HUB 节点：repeat 机制 + AtomicBoolean CAS 并发保护。
 * 3. 防资损：benefitGranted / userReached 标志位。
 * 4. 所有阻塞调用（Handler）切换到虚拟线程调度器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DagEngine {

    private final HandlerRegistry handlerRegistry;

    /** 用于阻塞型 Handler 调用（DAG 调度本身不阻塞 EventLoop） */
    private static final reactor.core.scheduler.Scheduler VIRTUAL =
            Schedulers.fromExecutorService(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());

    // ── 执行入口 ──────────────────────────────────────────────────

    /**
     * 从指定触发器节点开始执行整个画布 DAG。
     *
     * @param graph      已解析的 DAG
     * @param triggerNodeId 触发器节点 ID
     * @param ctx        执行上下文
     * @return Mono<Map> 终止节点的返回数据（业务直调场景），异步触发场景忽略
     */
    public Mono<Map<String, Object>> execute(DagGraph graph, String triggerNodeId, ExecutionContext ctx) {
        return executeNode(graph, triggerNodeId, ctx)
                .onErrorResume(e -> {
                    log.error("[ENGINE] 执行出错 executionId={}: {}", ctx.getExecutionId(), e.getMessage(), e);
                    return Mono.just(Map.of("error", e.getMessage()));
                });
    }

    // ── 单节点执行（6 个阶段） ─────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> executeNode(DagGraph graph, String nodeId, ExecutionContext ctx) {
        DagParser.CanvasNode node = graph.getNode(nodeId);
        if (node == null) return Mono.just(Map.of());

        Map<String, Object> rawConfig = node.getConfig() != null ? node.getConfig() : Map.of();

        return Mono.defer(() -> {

            // ── 阶段 1：解析配置（CONTEXT 字段替换为实际值）
            Map<String, Object> config = resolveConfig(rawConfig, ctx);

            // ── 阶段 2：LOGIC_RELATION / HUB 特殊处理
            if ("LOGIC_RELATION".equals(node.getType()) || "HUB".equals(node.getType())) {
                List<String> upstreamIds = graph.upstream(nodeId);

                if ("LOGIC_RELATION".equals(node.getType())) {
                    String relation = (String) config.getOrDefault("relation", "AND");
                    if (LogicRelationHandler.shouldFailImmediately(relation, upstreamIds, ctx)) {
                        ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                        return handleFailure(ctx, "LOGIC_RELATION AND 模式：上游节点失败/跳过");
                    }
                    if (!LogicRelationHandler.checkCondition(relation, upstreamIds, ctx)) {
                        return Mono.just(Map.of()); // 等待其他上游
                    }
                } else { // HUB
                    if (!HubHandler.allUpstreamDone(upstreamIds, ctx)) {
                        return Mono.just(Map.of()); // 等待其他上游
                    }
                }
            }

            // ── 阶段 3：幂等检查
            if (ctx.isNodeDone(nodeId)) {
                log.debug("[ENGINE] 节点已执行过，跳过 nodeId={}", nodeId);
                return Mono.just(Map.of());
            }

            // ── 阶段 4：CAS 抢占本地锁
            AtomicBoolean lock = ctx.getLock(nodeId);
            if (!lock.compareAndSet(true, false)) {
                // 其他协程持锁，设 waitProcess=true 后退出
                lock.set(true);
                return Mono.just(Map.of());
            }

            // ── 阶段 5：调用 Handler 执行业务逻辑
            NodeHandler handler = handlerRegistry.get(node.getType());
            return Mono.fromCallable(() -> handler.execute(config, ctx))
                    .subscribeOn(VIRTUAL)
                    .flatMap(result -> {
                        if (!result.success()) {
                            lock.set(true);
                            ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                            if (ctx.isBenefitGranted() || ctx.isUserReached()) {
                                log.warn("[ENGINE] 防资损：节点 {} 失败但已发券/触达，整体判定成功", nodeId);
                                return Mono.just(Map.of());
                            }
                            return handleFailure(ctx, "节点 " + nodeId + " 执行失败: " + result.errorMessage());
                        }

                        // 更新防资损标志
                        if (handler.isBenefitNode()) ctx.setBenefitGranted(true);
                        if (handler.isReachNode())   ctx.setUserReached(true);

                        // 写入节点输出
                        if (result.output() != null && !result.output().isEmpty()) {
                            ctx.putNodeOutput(nodeId, result.output());
                        }

                        // ── 阶段 6：检查是否需要 repeat（有其他协程在等待）
                        boolean needRepeat = lock.getAndSet(true); // lock 置回 true

                        ctx.setNodeStatus(nodeId, NodeStatus.SUCCESS);
                        log.debug("[ENGINE] 节点完成 nodeId={} type={}", nodeId, node.getType());

                        // 触发下游（并行）
                        Mono<Map<String, Object>> downstream = triggerDownstream(graph, result, nodeId, ctx);

                        if (needRepeat) {
                            // 再执行一次（处理等待协程带来的状态变更）
                            return downstream.then(executeNode(graph, nodeId, ctx));
                        }
                        return downstream;
                    })
                    .onErrorResume(e -> {
                        lock.set(true);
                        ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                        log.error("[ENGINE] 节点异常 nodeId={}: {}", nodeId, e.getMessage());
                        if (ctx.isBenefitGranted() || ctx.isUserReached()) return Mono.just(Map.of());
                        return Mono.error(e);
                    });
        });
    }

    // ── 触发下游节点（并行） ──────────────────────────────────────

    private Mono<Map<String, Object>> triggerDownstream(
            DagGraph graph, NodeResult result, String currentNodeId, ExecutionContext ctx) {

        List<String> nextIds = collectNextIds(result, currentNodeId);
        if (nextIds.isEmpty()) return Mono.just(result.output() != null ? result.output() : Map.of());

        return Flux.fromIterable(nextIds)
                .flatMap(nextId -> executeNode(graph, nextId, ctx))
                .last(Map.of())
                .defaultIfEmpty(Map.of());
    }

    private List<String> collectNextIds(NodeResult result, String currentNodeId) {
        List<String> ids = new ArrayList<>();
        if (result.nextNodeId() != null)    ids.add(result.nextNodeId());
        if (result.successNodeId() != null) ids.add(result.successNodeId());
        if (result.failNodeId() != null)    ids.add(result.failNodeId());
        if (result.elseNodeId() != null)    ids.add(result.elseNodeId());
        if (result.branchMap() != null)     ids.addAll(result.branchMap().values());
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    // ── 防资损失败处理 ────────────────────────────────────────────

    private Mono<Map<String, Object>> handleFailure(ExecutionContext ctx, String reason) {
        if (ctx.isBenefitGranted() || ctx.isUserReached()) {
            log.warn("[ENGINE] 防资损生效：{}", reason);
            return Mono.just(Map.of());
        }
        return Mono.error(new RuntimeException(reason));
    }

    // ── 配置解析：CONTEXT 字段替换为实际值 ───────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveConfig(Map<String, Object> config, ExecutionContext ctx) {
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
