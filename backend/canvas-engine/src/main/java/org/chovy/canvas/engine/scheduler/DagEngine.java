package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.execution.CanvasExecutionDlq;
import org.chovy.canvas.domain.execution.CanvasExecutionDlqMapper;
import org.chovy.canvas.domain.execution.CanvasExecutionTrace;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeGate;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handler.HandlerRegistry;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeRouteResolver;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.handlers.HubHandler;
import org.chovy.canvas.engine.handlers.LogicRelationHandler;
import org.chovy.canvas.infra.redis.ContextPersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * DAG 执行调度器（精确实现设计文档第七章）。
 *
 * <h3>执行隔离模型</h3>
 * <pre>
 * 每次 trigger() 调用都会创建或加载独立的 ExecutionContext，以 (canvasId, userId) 为 key。
 * 不同用户、不同触发 之间完全隔离，互不干扰：
 *
 *                      ┌─ ctx_A ──→ nodeA1 → nodeA2 → ...
 *   DagGraph（只读）───┤
 *                      └─ ctx_B ──→ nodeB1 → nodeB2 → ...
 *
 * DagGraph 是只读的节点配置，全局共享（从缓存取）。
 * 各节点的执行状态、输出数据、并发锁（NodeGate）均存储在各自的 ExecutionContext 里，
 * 不同执行之间从不共享状态。
 * </pre>
 *
 * <h3>repeat / NodeGate 的作用范围</h3>
 * <pre>
 * repeat 机制只处理 intra-execution（单次执行内部）的并行分支汇聚问题，
 * 即同一个 ctx 里的多条并行分支同时到达同一个汇聚节点的情况：
 *
 *   分支1 ──→ 汇聚节点 ← 只能执行一次（repeat 保障）
 *   分支2 ──┘
 *
 * repeat 在 THRESHOLD 节点有真正的语义价值：
 * ThresholdHandler 不等所有上游完成，每次触发都读 ctx 计数。
 * 持锁期间到来的上游通过 repeatPending 被捕获，repeat 重新计数后可能触发路由。
 * 没有 repeat 则该信号永久丢失，节点卡在 WAITING 直到超时。
 *
 * 对于 HUB / LogicRelation / AGGREGATE：均用 allUpstreamDone 门控，
 * repeat 是防御性兜底，没有改变路由结果的实际效果。
 *
 * 对于线性或树状结构（每个节点只有一条入边），不存在并发到达，
 * NodeGate.repeatPending 永远是 false，repeat 逻辑完全不触发。
 * </pre>
 *
 * <h3>核心机制</h3>
 * 1. 单节点 6 阶段执行（7.4节）<br>
 * 2. repeat 并发保护（7.5节）—— 在写 SUCCESS 之前检查 repeat，防止幂等拦截<br>
 * 3. LOGIC_RELATION AND 模式立即失败（7.7节）<br>
 * 4. Hub 超时延迟任务（设计文档 Hub 定义）<br>
 * 5. Priority 串行依序尝试（4.6节）<br>
 * 6. 执行结束批量写入 SKIPPED（7.7节）
 */
@Slf4j
@Component
public class DagEngine {

    private final HandlerRegistry handlerRegistry;
    private final TraceWriteBuffer traceBuffer;
    private final CanvasExecutionDlqMapper dlqMapper;
    private final CircuitBreakerRegistry cbRegistry;
    private final CanvasMetrics metrics;
    private final ObjectMapper objectMapper;
    private final ContextPersistenceService ctxStore;
    // @Lazy 避免与 CanvasExecutionService → DagEngine 的循环依赖
    private final org.chovy.canvas.engine.trigger.CanvasExecutionService executionService;

    public DagEngine(HandlerRegistry handlerRegistry,
                     TraceWriteBuffer traceBuffer,
                     CanvasExecutionDlqMapper dlqMapper,
                     CircuitBreakerRegistry cbRegistry,
                     CanvasMetrics metrics,
                     ObjectMapper objectMapper,
                     ContextPersistenceService ctxStore,
                     @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService) {
        this.handlerRegistry = handlerRegistry;
        // traceMapper 保留供 writeSkippedNodes 直接写入；批量写走 traceBuffer
        this.traceBuffer = traceBuffer;
        this.dlqMapper = dlqMapper;
        this.cbRegistry = cbRegistry;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.ctxStore = ctxStore;
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
    private static final Scheduler VIRTUAL =
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
        // 从起始节点开始执行画布相关节点
        return executeNode(graph, triggerNodeId, ctx, 0)
                .doFinally(__ -> writeSkippedNodesIfComplete(graph, ctx))
                .onErrorResume(e -> {
                    log.error("[ENGINE] 执行出错 executionId={}: {}",
                            ctx.getExecutionId(), e.getMessage(), e);
                    return Mono.just(Map.of(MapFieldKeys.ERROR, e.getMessage()));
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

            boolean needsNodeId = NodeType.MANUAL_APPROVAL.equals(node.getType())
                    || NodeType.API_CALL.equals(node.getType())
                    || NodeType.WAIT.equals(node.getType())
                    || NodeType.GOAL_CHECK.equals(node.getType())
                    || NodeType.FREQUENCY_CAP.equals(node.getType())
                    || NodeType.SEND_EMAIL.equals(node.getType())
                    || NodeType.SEND_SMS.equals(node.getType())
                    || NodeType.SEND_PUSH.equals(node.getType())
                    || NodeType.SEND_IN_APP.equals(node.getType())
                    || NodeType.SEND_WECHAT.equals(node.getType())
                    || NodeType.POINTS_OPERATION.equals(node.getType())
                    || NodeType.LOOP.equals(node.getType())
                    || NodeType.GOTO.equals(node.getType());
            Map<String, Object> config = needsNodeId
                    ? resolveConfigWithNodeId(rawConfig, ctx, nodeId, node.getType())
                    : resolveConfig(rawConfig, ctx);

            // ──────────────────────────────────────────────────────
            // 阶段 2：LOGIC_RELATION / HUB / AGGREGATE / THRESHOLD 特殊处理
            // ──────────────────────────────────────────────────────
            if (NodeType.LOGIC_RELATION.equals(node.getType())) {
                return handleLogicRelation(graph, nodeId, node, config, ctx, depth);
            }
            if (NodeType.HUB.equals(node.getType())) {
                return handleHub(graph, nodeId, node, config, ctx, depth);
            }
            if (NodeType.AGGREGATE.equals(node.getType())) {
                return handleAggregate(graph, nodeId, node, config, ctx, depth);
            }
            if (NodeType.THRESHOLD.equals(node.getType())) {
                // THRESHOLD 不等所有上游完成——每个上游完成都触发一次 handler。
                // 与 HUB/AGGREGATE 的关键区别：此处只注入 upstreamIds，不加 allUpstreamDone 门控，
                // handler 在执行时才读 ctx 计数，这是 repeat 机制真正有用的场景：
                // 持锁期间到来的上游信号通过 repeatPending 被捕获，repeat 重新评估后正确路由。
                Map<String, Object> enrichedConfig = new HashMap<>(config);
                enrichedConfig.put(MapFieldKeys.UPSTREAM_IDS, graph.upstream(nodeId));
                enrichedConfig.put(MapFieldKeys.NODE_ID_INTERNAL, nodeId);
                scheduleThresholdTimeoutIfNeeded(nodeId, config, ctx);
                return executeNodeAfterStage2(graph, nodeId, node, enrichedConfig, ctx, depth);
            }

            // ──────────────────────────────────────────────────────
            // 阶段 3：幂等检查（已执行过则跳过）
            // ──────────────────────────────────────────────────────
            if (ctx.isNodeDone(nodeId)) {
                log.debug("[ENGINE] 幂等跳过 nodeId={}", nodeId);
                return Mono.just(Map.of());
            }

            // FIXME: 此处CAS锁其实没有覆盖之前特殊类型的节点, 但是是否真正有竞争的是上面的特殊节点？相反如果是普通节点的话, 发生并发的概率其实不大
            // ──────────────────────────────────────────────────────
            // 阶段 4：CAS 抢占 nodeGate 门控锁
            // ──────────────────────────────────────────────────────
            NodeGate nodeGate = ctx.getGate(nodeId);
            if (!nodeGate.executing.compareAndSet(false, true)) {
                // 抢锁失败：set(true) 向持锁协程发送 repeat 信号
                nodeGate.repeatPending.set(true);
                log.debug("[ENGINE] CAS 失败，发出 repeat 信号 nodeId={}", nodeId);
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

            // 具体普通节点执行逻辑
            return executeHandlerWithRepeat(handler, config, ctx, nodeGate,
                    nodeId, node.getType())
                    .<Map<String, Object>>flatMap(result -> {

                        if (!result.success()) {
                            // Handler 返回失败
                            nodeGate.executing.set(false); // 释放锁
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
                        if (handler.isReachNode()) ctx.setUserReached(true);
                        if (result.output() != null && !result.output().isEmpty()) {
                            ctx.putNodeOutput(nodeId, result.output());
                        }

                        NodeStatus status = statusForOutcome(result.outcome());
                        ctx.setNodeStatus(nodeId, status);
                        long durationMs = System.currentTimeMillis() - nodeStartMs;
                        writeTraceEnd(ctx, node, result, durationMs);
                        metrics.recordNodeExecution(node.getType(), status.name(), durationMs);
                        log.debug("[ENGINE] 节点完成 nodeId={} type={}", nodeId, node.getType());

                        if (result.pending()) {
                            return Mono.just(pendingResponse(nodeId, node.getType(), result));
                        }

                        // 触发下游逻辑执行
                        return triggerDownstream(graph, result, nodeId, node.getType(), ctx, depth);
                    })
                    .onErrorResume(e -> {
                        nodeGate.executing.set(false); // 释放异常锁
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

    /**
     * 带 repeat 保护的 Handler 执行（含熔断、重试、DLQ）。
     *
     * <h3>为什么不给每个并发触发各复制一份节点独立执行？</h3>
     * <pre>
     * 直觉上让 A、B 各自拿一份节点独立跑似乎更简单，但这会导致下游被触发两次（双执行/资损）。
     * ExecutionContext 是单次执行内共享的，复制出的两份还会并发读写同一 ctx。
     * 因此汇聚节点必须串行执行，repeat 解决"最后到达者不被漏掉"的边角场景。
     * </pre>
     *
     * <h3>repeat 有实际语义价值的节点：THRESHOLD</h3>
     * <pre>
     * THRESHOLD 不等所有上游完成，每个上游到来都直接运行 handler。
     * handler 读 ctx 计数，未达阈值返回 waiting()，达到阈值才路由。
     *
     * repeat 必要的场景（threshold=3，当前 2 个完成）：
     *   上游2完成 → handler 持锁 → 计数=2 < 3 → 返回 waiting()
     *   上游3在持锁期间到达 → CAS 失败 → repeatPending=true
     *   handler 返回 → repeat 触发 → 重新计数=3 ≥ 3 → 路由 ✓
     *
     * 没有 repeat：上游3的信号永久丢失，节点卡在 WAITING，只能等超时恢复。
     *
     * </pre>
     *
     * <h3>HUB / LogicRelation / AGGREGATE：repeat 无实际语义价值</h3>
     * <pre>
     * 这三者都用 allUpstreamDone 门控，只有在所有上游都完成后才进入 handler。
     * 而上游写输出到 ctx 发生在触发下游之前，因此 handler 执行时 ctx 已完整。
     * repeat 对它们只是防御性兜底（确保并发路由请求不丢失），不改变最终结果。
     * handler 执行时 ctx 已完整，repeat 是防御性兜底，不改变路由结果。
     * </pre>
     *
     * <h3>三种到达场景（Case 1/2/3）</h3>
     * <pre>
     * Case 1 — 无并发：
     *   A 抢到锁 → 执行 → 读 repeatPending=false → 释放锁 → 无 repeat
     *
     * Case 2 — 有并发（B 在 A 执行期间到达）：
     *   B CAS 失败 → 设 repeatPending=true
     *   上游3在持锁期间到达 → CAS 失败 → repeatPending=true（此时 B 的 ctx 变更已可见）
     *
     * Case 3 — 竞态边界（B 在 A 释放锁后才到）：
     *   A 释放锁 → B CAS 成功 → B 自己执行 → A 的 repeat CAS 失败 → 不重复
     * </pre>
     *
     * @param nodeGate 节点执行门控，含独立的互斥锁（executing）和 repeat 信号（repeatPending）
     */
    // retryWhen 的链式调用在 Java 类型推断中会丢失泛型参数（见方法体内的 retried 转型）。
    private Mono<NodeResult> executeHandlerWithRepeat(NodeHandler handler,
                                                      Map<String, Object> config,
                                                      ExecutionContext ctx,
                                                      NodeGate nodeGate,
                                                      String nodeId,
                                                      String nodeType) {
        CircuitBreakerRegistry.CircuitBreaker cb = cbRegistry.get(nodeType);

        // ── ① singleCall：单次 handler 调用 ────────────────────────
        // 必须用 Mono.defer 而不是 Mono.just(handler.executeAsync(...))。
        // Mono.just 在定义时就立即求值，后续每次订阅返回的是同一个缓存结果；
        // Mono.defer 在每次被订阅时重新执行 lambda，repeat 时才能真正再调一次 handler。
        // → repeat 能生效的前提就在这里。
        Mono<NodeResult> singleCall = Mono.defer(() -> {
                    try {
                        cb.checkState(); // 熔断 OPEN 时抛异常，不可重试
                    } catch (CircuitBreakerRegistry.CircuitBreakerOpenException e) {
                        return Mono.just(NodeResult.fail(e.getMessage()));
                    }
                    return handler.executeAsync(config, ctx); // ← handler 真正在这里被调用
                })
                .doOnNext(r -> {
                    if (r.success()) cb.recordSuccess();
                    else cb.recordFailure();
                })
                .doOnError(e -> cb.recordFailure());

        // ── ② withRetry：singleCall + 指数退避重试 + DLQ 兜底 ────
        // withRetry 本身也是惰性的（基于 singleCall 的 Mono.defer）：
        // 每次被订阅都会重新走一遍 singleCall → handler.executeAsync。
        // repeat 时正是通过重新订阅 withRetry 来再次执行 handler。
        //
        // retryWhen 的泛型在 Java 类型推断中会丢失，导致后续算子拿到 raw Mono。
        // 用显式转型将类型信息补回来，onErrorResume 中的 e 就能被推断为 Throwable。
        Mono<NodeResult> retried = singleCall
                .retryWhen(Retry.backoff(maxRetry, Duration.ofMillis(retryBaseDelayMs))
                        .maxBackoff(Duration.ofMillis(retryMaxDelayMs))
                        .filter(this::isRetryable)
                        .doBeforeRetry(sig -> {
                            metrics.recordNodeRetry(nodeType);
                            log.warn("[ENGINE] 节点重试 nodeId={} attempt={} reason={}",
                                    nodeId, sig.totalRetries() + 1, sig.failure().getMessage());
                        }));

        Mono<NodeResult> withRetry = retried.onErrorResume(e -> {
            writeDlq(ctx, nodeId, nodeType, e);
            return Mono.just(NodeResult.fail("已写入DLQ: " + e.getMessage()));
        });

        // ── ③ repeat 保护：handler 执行完后检查并发信号 ────────────
        // 执行链说明：
        //   withRetry 被订阅（第一次）→ singleCall → handler.executeAsync（第一次调用）
        //   → 结果进入 flatMap 的 lambda
        //   → 若 needsRepeat=true：lambda 返回 withRetry.doFinally(__)
        //   → flatMap 自动订阅这个新返回的 Mono（这就是 repeat 的触发点）
        //   → withRetry 被订阅（第二次）→ singleCall → handler.executeAsync（第二次调用）
        //   → doFinally 在第二次完成后执行，释放 executing 锁
        //
        // 注意：doFinally 本身不触发订阅，触发订阅的是 flatMap 对返回值的处理。
        return withRetry.flatMap(result -> {
            if (!result.success()) {
                // 失败时调用方（executeNodeAfterStage2）会释放 nodeGate
                return Mono.just(result);
            }

            // 先读并清除 repeatPending，再释放 executing 锁。
            // 顺序关键：必须在释放锁之前读信号，否则新协程抢锁后才来的请求会被遗漏。
            // （详见 executeHandlerWithRepeat JavaDoc 中的 Case 3 分析）
            boolean needsRepeat = nodeGate.repeatPending.getAndSet(false); // ← 读信号
            nodeGate.executing.set(false); // 释放锁

            if (needsRepeat && nodeGate.executing.compareAndSet(false, true)) {
                // 重新持锁后返回 withRetry.doFinally(__)：
                //   flatMap 订阅它 → handler 第二次执行（repeat）
                //   doFinally → repeat 结束后释放锁（不管成功/失败/取消）
                log.debug("[ENGINE] repeat nodeId={}", nodeId);
                return withRetry.doFinally(__ -> nodeGate.executing.set(false));
            }

            // needsRepeat=false，或 CAS 失败（另一协程已接管）：不 repeat
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
                    .perfRunId(ctx.getPerfRunId())
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
                    .subscribe(null, (Throwable e) -> log.error("[DLQ] 写入失败: {}", e.getMessage()));
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
                                                          ExecutionContext ctx,
                                                          int depth) {
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
            if (ctx.getScheduledHubTimeouts().add("lr:" + nodeId)) {
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
                                                (Throwable e) -> log.error("[LOGIC_RELATION] 超时恢复失败 nodeId={}: {}", nodeId, e.getMessage()));
                            }
                        });
                log.debug("[LOGIC_RELATION] 启动等待超时定时器 {}s nodeId={}", timeoutSec, nodeId);
            }
            log.debug("[ENGINE] LOGIC_RELATION 条件未满足，进入 WAITING nodeId={}", nodeId);
            return Mono.just(Map.of());
        }

        // 条件满足 → 走正常节点执行流程（阶段 3-6）
        return executeNodeAfterStage2(graph, nodeId, node, config, ctx, depth);
    }

    @org.springframework.beans.factory.annotation.Value("${canvas.execution.global-timeout-sec:600}")
    private long globalTimeout;

    // ══════════════════════════════════════════════════════════════
    // HUB 处理（含超时延迟任务，设计文档 HUB 节点说明）
    // ══════════════════════════════════════════════════════════════

    /**
     * 核心机制：每个上游节点完成时，主动调用 executeNode(HUB)
     * <br/>
     * 上游A完成 → executeNode(HUB) → allUpstreamDone=false → WAITING → 返回空Map
     * 上游B完成 → executeNode(HUB) → allUpstreamDone=true  → 继续执行
     * <br/>
     * 触发重入的是 DagEngine 的 CAS repeat 机制：
     * 核心机制：每个上游节点完成时，主动调用 executeNode(HUB)
     * <br/>
     * 上游A完成 → executeNode(HUB) → allUpstreamDone=false → WAITING → 返回空Map
     * 上游B完成 → executeNode(HUB) → allUpstreamDone=true  → 继续执行
     * <br/>
     * 触发重入的是 DagEngine 的 CAS repeat 机制:
     * 完整流程是这样的：
     * <br/>
     * 上游A完成
     * └> executeNode(HUB)
     * ├─ 抢到 CAS 锁
     * ├─ allUpstreamDone=false → WAITING → 返回 Map.of()
     * └─ 检查 nodeGate → false → 结束
     * 上游B完成
     * └> executeNode(HUB)
     * ├─ 抢锁失败（A还没释放）→ set(true) 发送信号 → 返回
     * 或者
     * ├─ 抢锁成功（A已释放）
     * ├─ allUpstreamDone=true → 继续执行下游
     * └─ 正常结束
     * <br/>
     * 若抢锁失败路径：A 在释放锁前发现 nodeGate=true（有信号）
     * └> repeat：A 重新执行 handleHub → allUpstreamDone=true → 继续
     * <br/>
     * Mono.delay() 是超时兜底，不是主驱动，它只做一件事：N 秒后如果 Hub 还没完成就标记 FAILED。
     * <br/>
     *
     */
    private Mono<Map<String, Object>> handleHub(DagGraph graph, String nodeId,
                                                DagParser.CanvasNode node,
                                                Map<String, Object> config,
                                                ExecutionContext ctx,
                                                int depth) {
        List<String> upstreamIds = graph.upstream(nodeId);

        // 所有上游未完成：进入等待态
        if (!HubHandler.allUpstreamDone(upstreamIds, ctx)) {
            ctx.setNodeStatus(nodeId, NodeStatus.WAITING);  // 设 WAITING 供 isPaused 检测
            if (ctx.getScheduledHubTimeouts().add(nodeId)) {
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
                                                (Throwable e) -> log.error("[HUB] 超时恢复失败 nodeId={}: {}", nodeId, e.getMessage()));
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
        return executeNodeAfterStage2(graph, nodeId, node, config, ctx, depth);
    }

    /**
     * 聚合评估节点处理（AGGREGATE）。
     *
     * <p>等待机制与 HUB 相同：所有上游完成前挂起，完成后进入正常执行阶段。
     * 差异在于进入 executeNodeAfterStage2 时会注入 {@code __upstreamIds}，
     * 供 {@link org.chovy.canvas.engine.handlers.AggregateHandler} 读取上游结果进行评估。
     *
     * <p>这也是 repeat 机制真正有意义的场景：
     * AggregateHandler 读取 ctx 中各上游的状态和输出来做判断。
     * 若多条上游并发完成，repeat 保证最后一次执行时 ctx 已包含所有上游的结果。
     */
    private Mono<Map<String, Object>> handleAggregate(DagGraph graph, String nodeId,
                                                      DagParser.CanvasNode node,
                                                      Map<String, Object> config,
                                                      ExecutionContext ctx,
                                                      int depth) {
        List<String> upstreamIds = graph.upstream(nodeId);

        if (!HubHandler.allUpstreamDone(upstreamIds, ctx)) {
            ctx.setNodeStatus(nodeId, NodeStatus.WAITING);
            String timerKey = "ag:" + nodeId;
            if (ctx.getScheduledHubTimeouts().add(timerKey)) {
                ctx.getHubStartTimes().putIfAbsent(timerKey, System.currentTimeMillis());
                int timeoutSec = config.get("timeout") instanceof Number n ? n.intValue() : (int) globalTimeout;

                Mono.delay(Duration.ofSeconds(timeoutSec), VIRTUAL)
                        .subscribe(__ -> {
                            if (!ctx.isNodeDone(nodeId)) {
                                log.warn("[AGGREGATE] 等待超时 timeout={}s nodeId={}", timeoutSec, nodeId);
                                ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                                ctxStore.save(ctx);
                                executionService.trigger(
                                                ctx.getCanvasId(), ctx.getUserId(),
                                                TriggerType.AGGREGATE_TIMEOUT, NodeType.AGGREGATE,
                                                null, Map.of(),
                                                ctx.getExecutionId() + ":ag-timeout:" + nodeId, false)
                                        .subscribe(null,
                                                (Throwable e) -> log.error("[AGGREGATE] 超时恢复失败 nodeId={}: {}", nodeId, e.getMessage()));
                            }
                        });
                log.debug("[AGGREGATE] 启动超时定时器 {}s nodeId={}", timeoutSec, nodeId);
            }
            return Mono.just(Map.of());
        }

        // 所有上游完成：将 upstreamIds 注入 config，供 AggregateHandler 读取上游结果
        Map<String, Object> enrichedConfig = new HashMap<>(config);
        enrichedConfig.put(MapFieldKeys.UPSTREAM_IDS, upstreamIds);
        return executeNodeAfterStage2(graph, nodeId, node, enrichedConfig, ctx, depth);
    }

    // ══════════════════════════════════════════════════════════════
    // 阶段 3-6 公共入口（LOGIC_RELATION / HUB / AGGREGATE 满足条件后调用）
    // ══════════════════════════════════════════════════════════════

    /**
     * THRESHOLD 超时调度（首次触发时启动，防止所有上游都不完成导致永久 WAITING）
     */
    private void scheduleThresholdTimeoutIfNeeded(String nodeId, Map<String, Object> config,
                                                  ExecutionContext ctx) {
        String timerKey = "th:" + nodeId;
        if (!ctx.getScheduledHubTimeouts().add(timerKey)) return;
        int timeoutSec = config.get("timeout") instanceof Number n ? n.intValue() : (int) globalTimeout;
        Mono.delay(Duration.ofSeconds(timeoutSec), VIRTUAL)
                .subscribe(__ -> {
                    if (!ctx.isNodeDone(nodeId)) {
                        log.warn("[THRESHOLD] 等待超时 timeout={}s nodeId={}", timeoutSec, nodeId);
                        ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                        ctxStore.save(ctx);
                        executionService.trigger(
                                        ctx.getCanvasId(), ctx.getUserId(),
                                        TriggerType.THRESHOLD_TIMEOUT, NodeType.THRESHOLD,
                                        null, Map.of(),
                                        ctx.getExecutionId() + ":th-timeout:" + nodeId, false)
                                .subscribe(null,
                                        (Throwable e) -> log.error("[THRESHOLD] 超时恢复失败 nodeId={}: {}", nodeId, e.getMessage()));
                    }
                });
    }

    private Mono<Map<String, Object>> executeNodeAfterStage2(DagGraph graph, String nodeId,
                                                             DagParser.CanvasNode node,
                                                             Map<String, Object> config,
                                                             ExecutionContext ctx,
                                                             int depth) {
        // 阶段 3：幂等
        if (ctx.isNodeDone(nodeId)) return Mono.just(Map.of());

        // 阶段 4：CAS
        NodeGate nodeGate = ctx.getGate(nodeId);
        if (!nodeGate.executing.compareAndSet(false, true)) {
            nodeGate.repeatPending.set(true);
            return Mono.just(Map.of());
        }

        writeTraceStart(ctx, node);
        NodeHandler handler = handlerRegistry.get(node.getType());
        long nodeStartMs = System.currentTimeMillis(); // 记录节点开始时间

        return executeHandlerWithRepeat(handler, config, ctx, nodeGate,
                nodeId, node.getType())
                .<Map<String, Object>>flatMap(result -> {
                    // ── 路径A：FAILED ─────────────────────────────────────────
                    // executeHandlerWithRepeat 对 FAILED 结果直接返回（跳过 repeat 检查），
                    // 锁由此处调用方释放。
                    if (!result.success()) {
                        nodeGate.executing.set(false); // 释放锁（FAILED 路径）
                        ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                        long durationMs = System.currentTimeMillis() - nodeStartMs;
                        writeTraceEnd(ctx, node, result, durationMs);
                        if (ctx.isBenefitGranted() || ctx.isUserReached()) return Mono.just(Map.of());
                        return Mono.error(new RuntimeException("节点 " + nodeId + " 失败: " + result.errorMessage()));
                    }
                    // ── 路径B：WAITING（THRESHOLD 阈值未满足）─────────────────
                    // executeHandlerWithRepeat 对 success=true 的结果会走 repeat 检查，
                    // 检查完后已执行 nodeGate.executing.set(false) 释放锁，此处不需再操作。
                    if (result.pending()) {
                        if (result.output() != null && !result.output().isEmpty()) {
                            ctx.putNodeOutput(nodeId, result.output());
                        }
                        NodeStatus status = statusForOutcome(result.outcome());
                        ctx.setNodeStatus(nodeId, status);
                        long durationMs = System.currentTimeMillis() - nodeStartMs;
                        writeTraceEnd(ctx, node, result, durationMs);
                        metrics.recordNodeExecution(node.getType(), status.name(), durationMs);
                        return Mono.just(pendingResponse(nodeId, node.getType(), result)); // 不触发下游，等待恢复
                    }
                    // ── 路径C：SUCCESS ────────────────────────────────────────
                    // 同路径B，锁已在 executeHandlerWithRepeat 中释放。
                    if (handler.isBenefitNode()) ctx.setBenefitGranted(true);
                    if (handler.isReachNode()) ctx.setUserReached(true);
                    if (result.output() != null && !result.output().isEmpty()) {
                        ctx.putNodeOutput(nodeId, result.output());
                    }
                    NodeStatus status = statusForOutcome(result.outcome());
                    ctx.setNodeStatus(nodeId, status);
                    long durationMs = System.currentTimeMillis() - nodeStartMs;
                    writeTraceEnd(ctx, node, result, durationMs);
                    metrics.recordNodeExecution(node.getType(), status.name(), durationMs);
                    log.debug("[ENGINE] 节点完成 nodeId={} type={}", nodeId, node.getType());
                    return triggerDownstream(graph, result, nodeId, node.getType(), ctx, depth);
                })
                .onErrorResume(e -> {
                    // 异常时锁可能仍被持有（handler 内部抛出），在此统一释放
                    nodeGate.executing.set(false);
                    ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                    if (ctx.isBenefitGranted() || ctx.isUserReached()) {
                        return Mono.just(Map.of());
                    }
                    return Mono.error(e);
                });
    }

    // ══════════════════════════════════════════════════════════════
    // 触发下游节点（含 Priority 串行逻辑，设计文档 4.6 节）
    // ══════════════════════════════════════════════════════════════

    private Mono<Map<String, Object>> triggerDownstream(DagGraph graph, NodeResult result,
                                                        String sourceNodeId, String sourceType,
                                                        ExecutionContext ctx,
                                                        int depth) {
        // 立即标记未走的分支入口为 SKIPPED（设计文档 7.7节两阶段写入 - 阶段一）
        // 目的：让 LOGIC_RELATION(AND) 在执行中即时检测到上游 SKIPPED，而不是等到执行结束
        markNonTakenBranchesSkipped(graph, sourceNodeId, sourceType, result, ctx);

        // PRIORITY 节点：串行依序尝试，第一个成功则停止（4.6节）
        if (NodeType.PRIORITY.equals(sourceType) && result.branchMap() != null) {
            String fallbackNextId = NodeRouteResolver.resolveFallbackTarget(result);
            List<String> orderedBranches = NodeRouteResolver.resolvePriorityBranchTargets(result);
            return tryPrioritySequentially(orderedBranches, fallbackNextId, sourceNodeId,
                    graph, ctx, depth + 1);
        }

        // 普通节点：收集所有下游并行触发
        List<String> nextIds = collectNextIds(result);
        if (nextIds.isEmpty()) {
            return Mono.just(result.output() != null ? result.output() : Map.of());
        }

        return Flux.fromIterable(nextIds)
                .flatMap(nextId -> executeNode(graph, nextId, ctx, depth + 1))
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
                                                              ExecutionContext ctx,
                                                              int depth) {
        if (branches.isEmpty()) {
            // 所有分支均失败
            if (fallbackNextId != null) {
                ctx.setNodeStatus(priorityNodeId, NodeStatus.PARTIAL_FAIL);
                log.debug("[PRIORITY] 所有分支失败，走 fallback nextId={}", fallbackNextId);
                if (ctx.isNodeDone(fallbackNextId)) {
                    log.debug("[PRIORITY] fallback 已作为分支执行过，按节点幂等跳过 nextId={}", fallbackNextId);
                    return Mono.just(Map.of());
                }
                return executeNode(graph, fallbackNextId, ctx, depth);
            }
            log.debug("[PRIORITY] 所有分支失败，无 fallback，整体 FAILED");
            return Mono.error(new RuntimeException("PRIORITY 所有分支均失败"));
        }

        String currentBranchId = branches.getFirst();
        return executeNode(graph, currentBranchId, ctx, depth)
                .<Map<String, Object>>flatMap(__ -> {
                    if (ctx.getNodeStatus(currentBranchId) == NodeStatus.SUCCESS) {
                        log.debug("[PRIORITY] 分支成功，停止 branchId={}", currentBranchId);
                        return Mono.just(Map.of());
                    }
                    // 当前分支失败，尝试下一个
                    log.debug("[PRIORITY] 分支失败，尝试下一个 branchId={}", currentBranchId);
                    return tryPrioritySequentially(
                            branches.subList(1, branches.size()),
                            fallbackNextId, priorityNodeId, graph, ctx, depth);
                })
                .onErrorResume(e -> {
                    if (!ctx.isNodeDone(currentBranchId)) {
                        return Mono.<Map<String, Object>>error(e);
                    }
                    log.debug("[PRIORITY] 分支异常结束，尝试下一个 branchId={} reason={}",
                            currentBranchId, e.getMessage());
                    return tryPrioritySequentially(
                            branches.subList(1, branches.size()),
                            fallbackNextId, priorityNodeId, graph, ctx, depth);
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

    private void writeSkippedNodesIfComplete(DagGraph graph, ExecutionContext ctx) {
        if (hasWaitingNodes(ctx)) {
            log.debug("[ENGINE] 执行已挂起，暂不批量写入 SKIPPED executionId={}", ctx.getExecutionId());
            return;
        }
        writeSkippedNodes(graph, ctx);
    }

    private boolean hasWaitingNodes(ExecutionContext ctx) {
        return ctx.getNodeStatuses().values().stream().anyMatch(status -> status == NodeStatus.WAITING);
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
        int status = traceStatus(result);
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
        return NodeRouteResolver.resolveTargets(result).stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private NodeStatus statusForOutcome(NodeOutcome outcome) {
        if (outcome == null) return NodeStatus.SUCCESS;
        return switch (outcome) {
            case FAIL -> NodeStatus.FAILED;
            case TIMEOUT -> NodeStatus.TIMEOUT;
            case SUPPRESSED -> NodeStatus.SUPPRESSED;
            case SKIPPED -> NodeStatus.SKIPPED;
            case PENDING -> NodeStatus.WAITING;
            case SUCCESS -> NodeStatus.SUCCESS;
        };
    }

    private int traceStatus(NodeResult result) {
        if (!result.success()) {
            return 2;
        }
        if (result.outcome() == NodeOutcome.PENDING) {
            return 0;
        }
        if (result.outcome() == NodeOutcome.SKIPPED) {
            return 3;
        }
        return 1;
    }

    private Map<String, Object> pendingResponse(String nodeId, String nodeType, NodeResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put(MapFieldKeys.PENDING, true);
        response.put(MapFieldKeys.NODE_ID, nodeId);
        response.put(MapFieldKeys.NODE_TYPE, nodeType);
        response.put(MapFieldKeys.OUTCOME, result.outcome().name());
        if (result.resumeAtEpochMs() != null) {
            response.put(MapFieldKeys.RESUME_AT_EPOCH_MS, result.resumeAtEpochMs());
        }
        if (result.reasonCode() != null) {
            response.put(MapFieldKeys.REASON_CODE, result.reasonCode());
        }
        if (result.reasonMessage() != null) {
            response.put(MapFieldKeys.REASON_MESSAGE, result.reasonMessage());
        }
        return response;
    }

    /**
     * 解析节点配置：将 valueType=CONTEXT 的字段替换为上下文实际值。
     * 设计文档 7.4 阶段 1。
     * 例如:
     * {
     * "apiKey": "send_sms",
     * "params": {
     * "phone": {
     * "valueType": "CONTEXT",
     * "value": "user.phone"  // ← 从上下文读取用户手机号
     * },
     * "content": {
     * "valueType": "CONTEXT",
     * "value": "trigger.message"  // ← 从触发事件读取消息内容
     * }
     * }
     * }
     * 会被处理为:
     * {
     * "apiKey": "send_sms",
     * "params": {
     * "phone": "13800138000",       // ← 实际手机号
     * "content": "您的订单已发货"    // ← 实际消息
     * }
     * }
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveConfig(Map<String, Object> config, ExecutionContext ctx) {
        if (config == null) return Map.of();

        // 性能优化：先检查是否有任何 CONTEXT 字段，无则直接返回原 Map 避免 HashMap 创建
        boolean hasContextField = config.values().stream().anyMatch(
                v -> v instanceof Map<?, ?> m && MapFieldKeys.CONTEXT.equals(m.get(MapFieldKeys.VALUE_TYPE)));
        if (!hasContextField) return config;   // O(n) 扫描，但避免了 HashMap allocation

        Map<String, Object> resolved = new HashMap<>(config);
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> m) {
                String valueType = (String) ((Map<String, Object>) m).get(MapFieldKeys.VALUE_TYPE);
                String value = (String) ((Map<String, Object>) m).get(MapFieldKeys.VALUE_KEY);
                if (MapFieldKeys.CONTEXT.equals(valueType) && value != null) {
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
                                                        ExecutionContext ctx, String nodeId,
                                                        String nodeType) {
        Map<String, Object> resolved = new HashMap<>(resolveConfig(config, ctx));
        resolved.put(MapFieldKeys.NODE_ID_INTERNAL, nodeId);
        enrichWaitResumePayload(resolved, ctx, nodeId, nodeType);
        return resolved;
    }

    private void enrichWaitResumePayload(Map<String, Object> resolved,
                                         ExecutionContext ctx,
                                         String nodeId,
                                         String nodeType) {
        Map<String, Object> payload = ctx.getTriggerPayload();
        Object sourceNodeId = payload.get(MapFieldKeys.SOURCE_NODE_ID);
        if (sourceNodeId != null && !nodeId.equals(sourceNodeId.toString())) {
            return;
        }
        if (NodeType.WAIT.equals(nodeType) && payload.containsKey(MapFieldKeys.WAIT_RESUME_STATUS)) {
            resolved.put(MapFieldKeys.WAIT_RESUME_STATUS, payload.get(MapFieldKeys.WAIT_RESUME_STATUS));
        }
        if (NodeType.GOAL_CHECK.equals(nodeType) && payload.containsKey(MapFieldKeys.GOAL_RESUME_STATUS)) {
            resolved.put(MapFieldKeys.GOAL_RESUME_STATUS, payload.get(MapFieldKeys.GOAL_RESUME_STATUS));
        }
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
            String successId = (String) cfg.get(MapFieldKeys.SUCCESS_NODE_ID);
            String failId = (String) cfg.get(MapFieldKeys.FAIL_NODE_ID);
            String takenId = result.successNodeId() != null ? result.successNodeId()
                    : result.failNodeId();
            String skippedId = takenId != null && takenId.equals(successId) ? failId : successId;
            markSkipped(skippedId, ctx);

        } else if ("SELECTOR".equals(sourceType)) {
            // 标记所有未走的 branch 入口
            java.util.List<Map<String, Object>> branches =
                    (java.util.List<Map<String, Object>>) cfg.getOrDefault(MapFieldKeys.BRANCHES, List.of());
            String elseId = (String) cfg.get(MapFieldKeys.ELSE_NODE_ID);
            String takenId = result.nextNodeId(); // SELECTOR 走的那条

            branches.forEach(b -> {
                String branchNext = (String) b.get(MapFieldKeys.NEXT_NODE_ID);
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
