package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.DataMaskingUtil;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
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
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /**
     * Fan-out batch submission with semaphore-based concurrency limiting.
     * Prevents large audience triggers from submitting all user executions at once.
     */
    public static final class FanOutBatcher {
        private final int batchSize;
        private final Semaphore semaphore;
        private final ExecutorService executor;

        public FanOutBatcher(int batchSize, int maxConcurrent) {
            if (batchSize <= 0) {
                throw new IllegalArgumentException("batchSize must be positive");
            }
            if (maxConcurrent <= 0) {
                throw new IllegalArgumentException("maxConcurrent must be positive");
            }
            this.batchSize = batchSize;
            this.semaphore = new Semaphore(maxConcurrent);
            this.executor = Executors.newVirtualThreadPerTaskExecutor();
        }

        public void fanOut(Stream<String> userIds, java.util.function.Consumer<List<String>> processor) {
            Objects.requireNonNull(userIds, "userIds must not be null");
            Objects.requireNonNull(processor, "processor must not be null");
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            List<String> batch = new ArrayList<>(batchSize);
            try (userIds) {
                userIds.forEach(userId -> {
                    batch.add(userId);
                    if (batch.size() == batchSize) {
                        futures.add(submitBatch(batch, processor));
                        batch.clear();
                    }
                });
            }
            if (!batch.isEmpty()) {
                futures.add(submitBatch(batch, processor));
            }
            futures.forEach(CompletableFuture::join);
        }

        public void shutdown() {
            executor.shutdown();
        }

        private CompletableFuture<Void> submitBatch(List<String> batch,
                                                    java.util.function.Consumer<List<String>> processor) {
            List<String> submittedBatch = List.copyOf(batch);
            acquirePermit();
            return CompletableFuture.runAsync(() -> {
                try {
                    processor.accept(submittedBatch);
                } finally {
                    semaphore.release();
                }
            }, executor);
        }

        private void acquirePermit() {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Fan-out interrupted", e);
            }
        }
    }

    /** 节点处理器注册表。 */
    private final HandlerRegistry handlerRegistry;
    /** 执行轨迹异步写入缓冲区。 */
    private final TraceWriteBuffer traceBuffer;
    /** 画布执行死信 Mapper。 */
    private final CanvasExecutionDlqMapper dlqMapper;
    /** 节点熔断器注册表。 */
    private final CircuitBreakerRegistry cbRegistry;
    /** 画布执行指标埋点器。 */
    private final CanvasMetrics metrics;
    /** Jackson ObjectMapper，用于节点输入输出序列化。 */
    private final ObjectMapper objectMapper;
    /** 执行上下文 Redis 持久化服务。 */
    private final ContextPersistenceService ctxStore;
    // @Lazy 避免与 CanvasExecutionService → DagEngine 的循环依赖
    /** 画布执行服务，用于超时恢复等内部触发。 */
    private final org.chovy.canvas.engine.trigger.CanvasExecutionService executionService;

    /**
     * 构造 DagEngine 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param handlerRegistry handlerRegistry 方法执行所需的业务参数
     * @param traceBuffer traceBuffer 方法执行所需的业务参数
     * @param dlqMapper dlqMapper 方法执行所需的业务参数
     * @param cbRegistry cbRegistry 方法执行所需的业务参数
     * @param metrics metrics 方法执行所需的业务参数
     * @param objectMapper objectMapper 方法执行所需的业务参数
     * @param ctxStore ctxStore 方法执行所需的业务参数
     * @param executionService executionService 方法执行所需的业务参数
     */
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

    /** 节点执行最大重试次数。 */
    @Value("${canvas.execution.max-retry:3}")
    private int maxRetry;

    /** 节点重试基础退避毫秒数。 */
    @Value("${canvas.execution.retry-base-delay-ms:1000}")
    private long retryBaseDelayMs;

    /** 节点重试最大退避毫秒数。 */
    @Value("${canvas.execution.retry-max-delay-ms:30000}")
    private long retryMaxDelayMs;

    /**
     * 特殊节点等待超时调度器。
     *
     * <p>Mono.delay 需要支持 time-based scheduling 的 Scheduler；
     * fromExecutorService 包装虚拟线程池不具备定时能力。
     */
    private static final Scheduler SPECIAL_NODE_TIMEOUT_SCHEDULER =
            Schedulers.newBoundedElastic(16, 10_000, "canvas-special-node-timeout", 60, true);

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
                .doOnTerminate(() -> writeSkippedNodesIfComplete(graph, ctx))
                .doOnError(e -> {
                    log.error("[ENGINE] 执行出错 executionId={}: {}",
                            ctx.getExecutionId(), e.getMessage(), e);
                    ctxStore.save(ctx);
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
                    || NodeType.COUPON.equals(node.getType())
                    || NodeType.POINTS_OPERATION.equals(node.getType())
                    || NodeType.COMMIT_ACTION.equals(node.getType())
                    || NodeType.LOOP.equals(node.getType())
                    || NodeType.GOTO.equals(node.getType())
                    || NodeType.TAGGER.equals(node.getType());
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
                Mono<Map<String, Object>> terminal = terminalSpecialNodeResult(nodeId, ctx);
                if (terminal != null) {
                    return terminal;
                }
                // THRESHOLD 不等所有上游完成——每个上游完成都触发一次 handler。
                // 与 HUB/AGGREGATE 的关键区别：此处只注入 upstreamIds，不加 allUpstreamDone 门控，
                // handler 在执行时才读 ctx 计数，这是 repeat 机制真正有用的场景：
                // 持锁期间到来的上游信号通过 repeatPending 被捕获，repeat 重新评估后正确路由。
                Map<String, Object> enrichedConfig = new HashMap<>(config);
                enrichedConfig.put(MapFieldKeys.UPSTREAM_IDS, graph.upstream(nodeId));
                enrichedConfig.put(MapFieldKeys.NODE_ID_INTERNAL, nodeId);
                scheduleThresholdTimeoutIfNeeded(graph, nodeId, node, config, ctx, depth);
                return executeNodeAfterStage2(graph, nodeId, node, enrichedConfig, ctx, depth);
            }

            // ──────────────────────────────────────────────────────
            // 阶段 3：幂等检查（已执行过则跳过）
            // ──────────────────────────────────────────────────────
            if (ctx.isNodeDone(nodeId)) {
                log.debug("[ENGINE] 幂等跳过 nodeId={}", nodeId);
                return Mono.just(Map.of());
            }

            // special 节点会先经过阶段 2 的等待/条件判断；一旦满足条件，会统一进入
            // executeNodeAfterStage2(...) 复用同一套幂等、CAS 与 repeat 机制。
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
                            // 锁已由 executeHandlerWithRepeat 释放，此处只处理状态和 trace。
                            ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                            writeTraceEnd(ctx, node, result, System.currentTimeMillis() - nodeStartMs);
                            saveNodeStateSafely(ctx, nodeId, NodeStatus.FAILED, result.output());
                            // 防资损：已发券/已触达则整体 SUCCESS
                            if (ctx.isBenefitGranted() || ctx.isUserReached()) {
                                log.warn("[ENGINE] 防资损：节点失败但整体判定成功 nodeId={}", nodeId);
                                return Mono.just(Map.of());
                            }
                            return triggerFailureAwareDownstream(graph, nodeId, node.getType(), ctx, depth,
                                    result.errorMessage());
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
                        saveNodeStateSafely(ctx, nodeId, status, result.output());
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
                        saveNodeStateSafely(ctx, nodeId, NodeStatus.FAILED, Map.of());
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
     * <h3>设计假设：普通节点在同一 execution 内只有一条活跃的上游 trigger</h3>
     * <pre>
     * repeat 机制的保护对象是"最后到达的信号不被漏掉"，它保证下游只触发一次、状态只写一次。
     * 但 handler 本身（executeAsync）在发生 CAS 竞争时会被调用两次：
     *   第一次：CAS 胜者持锁执行
     *   第二次：胜者释放锁后，因 repeatPending=true 重入执行
     *
     * 对于 HUB/LOGIC_RELATION/AGGREGATE：handler 是纯路由逻辑，无副作用，两次调用无害。
     * 对于 THRESHOLD：两次调用是语义必要的（第二次才能看到全部上游状态）。
     * 对于普通节点（发消息、发券、调 API 等）：若两条并发 trigger 同时到达且 handler
     *   执行期间存在竞争，handler 的副作用会执行两次。
     *
     * 因此，Canvas DAG 的设计约束是：
     *   多分支收敛必须经由显式的收敛节点（HUB / LOGIC_RELATION / AGGREGATE / THRESHOLD）。
     *   普通节点的直接上游在同一 execution 内只应有一条活跃路径，不应出现无收敛节点的菱形拓扑。
     *   若违反此约束，普通节点的副作用（发消息、扣库存等）可能被执行两次。
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
                        return Mono.just(NodeResult.circuitBreakerOpen(e.getMessage()));
                    }
                    try {
                        return handler.executeAsync(config, ctx) // ← handler 真正在这里被调用
                                .doOnError(e -> cb.recordFailure());
                    } catch (Throwable e) {
                        cb.recordFailure();
                        return Mono.error(e);
                    }
                })
                .doOnNext(r -> {
                    if (r.success()) cb.recordSuccess();
                    else if (!NodeResult.REASON_CIRCUIT_BREAKER_OPEN.equals(r.reasonCode())) {
                        cb.recordFailure();
                    }
                });

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
                // 失败时由此处统一释放锁，调用方 flatMap 不再重复释放，
                // 消除 repeat+failure 路径中 doFinally 与调用方双重释放的竞态窗口。
                nodeGate.executing.set(false);
                return Mono.just(result);
            }

            // 先读并清除 repeatPending，再释放 executing 锁。
            // 顺序关键：必须在释放锁之前读信号，否则新协程抢锁后才来的请求会被遗漏。
            // （详见 executeHandlerWithRepeat JavaDoc 中的 Case 3 分析）
            boolean needsRepeat = nodeGate.repeatPending.getAndSet(false); // ← 读信号
            nodeGate.executing.set(false); // 释放锁
            // 二次补检：捕获在 getAndSet→set(false) 窗口期内到达并设置 repeatPending 的信号。
            // 窗口期内 CAS 必然失败（锁仍被持有），对方已写入 repeatPending=true；
            // 释放锁后立即再读一次可以收住该信号，将窗口从"单次读→释放"缩小到"释放→二次读"。
            needsRepeat |= nodeGate.repeatPending.getAndSet(false);

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
     * 执行 terminal Special Node Result 对应的业务逻辑。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @param nodeId nodeId 对应的业务主键或标识
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    private Mono<Map<String, Object>> terminalSpecialNodeResult(String nodeId, ExecutionContext ctx) {
        NodeStatus status = ctx.getNodeStatus(nodeId);
        if (status == NodeStatus.FAILED || status == NodeStatus.TIMEOUT || status == NodeStatus.PARTIAL_FAIL) {
            // 特殊汇聚节点进入失败类终态后不再恢复执行，防止超时恢复重复改写路由结果。
            return Mono.error(new RuntimeException("节点 " + nodeId + " 已处于终态: " + status));
        }
        if (ctx.isNodeDone(nodeId)) {
            // 已完成的 special 节点按幂等返回，避免恢复触发重复写 trace 或重复下游路由。
            return Mono.just(Map.of());
        }
        return null;
    }

    /**
     * 写入死信队列（13.3节）
     */
    private void writeDlq(ExecutionContext ctx, String nodeId, String nodeType, Throwable cause) {
        metrics.recordDlq(nodeType);
        try {
            String msg = cause.getMessage() != null ? cause.getMessage() : "unknown";
            CanvasExecutionDlqDO dlq = CanvasExecutionDlqDO.builder()
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
            // DLQ 写入放到 boundedElastic，避免阻塞 Reactor 主执行链路。
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
        Mono<Map<String, Object>> terminal = terminalSpecialNodeResult(nodeId, ctx);
        if (terminal != null) {
            return terminal;
        }
        List<String> upstreamIds = graph.upstream(nodeId);
        String relation = (String) config.getOrDefault("relation", "AND");

        // AND 模式：上游有 FAILED/SKIPPED → 立即失败（7.7节）
        //
        // handleLogicRelation 在 executeNode 第2阶段提前返回，绕过了第3阶段（isNodeDone）和
        // 第4阶段（CAS），多条上游并发完成时多线程会同时进入此分支。
        // 用 CAS 保证 writeTrace 和 setStatus 只由一个线程执行；
        // 其他线程 CAS 失败后幂等返回，由赢家负责向外传播 Mono.error。
        if (LogicRelationHandler.shouldFailImmediately(relation, upstreamIds, ctx)) {
            NodeGate gate = ctx.getGate(nodeId);
            if (gate.executing.compareAndSet(false, true)) {
                writeTraceStart(ctx, node);
                ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                writeTraceEnd(ctx, node, NodeResult.fail("AND 上游失败，条件不可满足"), 0);
                saveNodeStateSafely(ctx, nodeId, NodeStatus.FAILED, Map.of());
                gate.executing.set(false);
                log.warn("[ENGINE] LOGIC_RELATION AND 上游失败，立即 FAILED nodeId={}", nodeId);
                if (ctx.isBenefitGranted() || ctx.isUserReached()) return Mono.just(Map.of());
                return Mono.error(new RuntimeException("LOGIC_RELATION AND 条件因上游失败不可满足"));
            }
            // CAS 失败：另一线程已在处理，当前线程幂等返回
            return Mono.just(Map.of());
        }

        // 条件未满足：进入等待态，设置 WAITING 状态（供 isPaused 检测）
        //
        // ── 并发安全性说明 ──────────────────────────────────────────
        // 此处没有加锁，多个上游并发完成时多条线程可能同时走到这里：
        //
        //   场景A（至少一条线程看到"条件已满足"）：
        //     该线程进入 executeNodeAfterStage2，由 stage 4 的 CAS 保证只执行一次。
        //
        //   场景B（所有线程都读到"条件未满足"，全部返回 WAITING）：
        //     这是真实可能发生的竞态——A、B 两个上游几乎同时完成，
        //     Thread-A 触发本节点时读到 B=PENDING，Thread-B 触发时读到 A=PENDING，
        //     两条线程都走了 WAITING 分支，没有人进入 executeNodeAfterStage2。
        //     此时节点会卡死，直到下方的超时定时器介入。
        //
        //   因此超时不是可选的"安全网"，而是并发正确性的必要兜底：
        //   超时触发新的 execution，最终驱动节点从 WAITING 状态恢复。
        //
        //   setNodeStatus(WAITING) 多线程同时写相同值，幂等，没有问题。
        // ──────────────────────────────────────────────────────────
        if (!LogicRelationHandler.checkCondition(relation, upstreamIds, ctx)) {
            if (ctx.setNodeStatusIfNotDone(nodeId, NodeStatus.WAITING)) {
                saveNodeStateSafely(ctx, nodeId, NodeStatus.WAITING, Map.of());
            }

            // LOGIC_RELATION 等待超时（与 Hub 类似，防止第二触发永不到来）
            // config 中可选配置 timeout 字段（秒），默认等于全局执行超时
            // ConcurrentHashSet.add() 保证定时器只被调度一次（多线程同时进入时）
            if (ctx.getScheduledHubTimeouts().add("lr:" + nodeId)) {
                ctx.getHubStartTimes().putIfAbsent("lr:" + nodeId, System.currentTimeMillis());
                int timeoutSec = config.get("timeout") instanceof Number n
                        ? n.intValue() : (int) globalTimeout;

                Mono.delay(Duration.ofSeconds(timeoutSec), SPECIAL_NODE_TIMEOUT_SCHEDULER)
                        .subscribe(__ -> handleSpecialNodeTimeout(graph, nodeId, node, config, ctx, depth,
                                "LOGIC_RELATION", timeoutSec));
                log.debug("[LOGIC_RELATION] 启动等待超时定时器 {}s nodeId={}", timeoutSec, nodeId);
            }
            log.debug("[ENGINE] LOGIC_RELATION 条件未满足，进入 WAITING nodeId={}", nodeId);
            return Mono.just(Map.of());
        }

        // 条件满足 → 走正常节点执行流程（阶段 3-6）
        return executeNodeAfterStage2(graph, nodeId, node, config, ctx, depth);
    }

    /** 特殊等待节点的全局超时秒数兜底。 */
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
        Mono<Map<String, Object>> terminal = terminalSpecialNodeResult(nodeId, ctx);
        if (terminal != null) {
            return terminal;
        }
        List<String> upstreamIds = graph.upstream(nodeId);

        // 所有上游未完成：进入等待态
        //
        // ── 并发安全性说明（与 LOGIC_RELATION 相同） ───────────────
        // 此处无锁：多条上游并发完成时，多条线程可能同时执行以下检查。
        //
        //   正常路径：最后一条线程必然看到 allUpstreamDone=true，
        //     进入 executeNodeAfterStage2，stage 4 CAS 保证只执行一次。
        //
        //   竞态路径（全部线程均读到"未全完成"）：
        //     节点卡在 WAITING，依赖下方超时定时器恢复。
        //     超时是并发正确性的必要兜底，不是可选项。
        //
        //   setNodeStatus(WAITING) 多线程写相同值，幂等。
        //   scheduledHubTimeouts.add() 是 ConcurrentHashSet，保证定时器只调度一次。
        // ──────────────────────────────────────────────────────────
        if (!HubHandler.allUpstreamDone(upstreamIds, ctx)) {
            if (ctx.setNodeStatusIfNotDone(nodeId, NodeStatus.WAITING)) {
                saveNodeStateSafely(ctx, nodeId, NodeStatus.WAITING, Map.of());
            }
            if (ctx.getScheduledHubTimeouts().add(nodeId)) {
                ctx.getHubStartTimes().putIfAbsent(nodeId, System.currentTimeMillis());
                int timeoutSec = HubHandler.getTimeoutSeconds(config);

                // 延迟任务：超时后若 Hub 仍未完成则标记 FAILED，持久化 ctx，触发执行恢复
                Mono.delay(Duration.ofSeconds(timeoutSec), SPECIAL_NODE_TIMEOUT_SCHEDULER)
                        .subscribe(__ -> handleSpecialNodeTimeout(graph, nodeId, node, config, ctx, depth,
                                "HUB", timeoutSec));

                log.debug("[HUB] 启动超时定时器 {}s nodeId={}", timeoutSec, nodeId);
            } else {
                // 已调度过定时器，检查是否已超时
                long start = ctx.getHubStartTimes().getOrDefault(nodeId, System.currentTimeMillis());
                int timeout = HubHandler.getTimeoutSeconds(config);
                if (System.currentTimeMillis() - start > (long) timeout * 1000) {
                    if (ctx.setNodeStatusIfNotDone(nodeId, NodeStatus.FAILED)) {
                        saveNodeStateSafely(ctx, nodeId, NodeStatus.FAILED, Map.of());
                    }
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
        Mono<Map<String, Object>> terminal = terminalSpecialNodeResult(nodeId, ctx);
        if (terminal != null) {
            return terminal;
        }
        List<String> upstreamIds = graph.upstream(nodeId);

        if (!HubHandler.allUpstreamDone(upstreamIds, ctx)) {
            // 并发安全性同 handleHub：无锁，竞态下超时定时器是必要兜底。
            if (ctx.setNodeStatusIfNotDone(nodeId, NodeStatus.WAITING)) {
                saveNodeStateSafely(ctx, nodeId, NodeStatus.WAITING, Map.of());
            }
            String timerKey = "ag:" + nodeId;
            if (ctx.getScheduledHubTimeouts().add(timerKey)) {
                ctx.getHubStartTimes().putIfAbsent(timerKey, System.currentTimeMillis());
                int timeoutSec = config.get("timeout") instanceof Number n ? n.intValue() : (int) globalTimeout;

                Mono.delay(Duration.ofSeconds(timeoutSec), SPECIAL_NODE_TIMEOUT_SCHEDULER)
                        .subscribe(__ -> handleSpecialNodeTimeout(graph, nodeId, node, config, ctx, depth,
                                "AGGREGATE", timeoutSec));
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
    private void scheduleThresholdTimeoutIfNeeded(DagGraph graph,
                                                  String nodeId,
                                                  DagParser.CanvasNode node,
                                                  Map<String, Object> config,
                                                  ExecutionContext ctx,
                                                  int depth) {
        String timerKey = "th:" + nodeId;
        if (!ctx.getScheduledHubTimeouts().add(timerKey)) return;
        int timeoutSec = config.get("timeout") instanceof Number n ? n.intValue() : (int) globalTimeout;
        Mono.delay(Duration.ofSeconds(timeoutSec), SPECIAL_NODE_TIMEOUT_SCHEDULER)
                .subscribe(__ -> handleSpecialNodeTimeout(graph, nodeId, node, config, ctx, depth,
                        "THRESHOLD", timeoutSec));
    }

    private void handleSpecialNodeTimeout(DagGraph graph,
                                          String nodeId,
                                          DagParser.CanvasNode node,
                                          Map<String, Object> config,
                                          ExecutionContext ctx,
                                          int depth,
                                          String label,
                                          int timeoutSec) {
        if (!ctx.setNodeStatusIfNotDone(nodeId, NodeStatus.TIMEOUT)) {
            return;
        }
        log.warn("[{}] 等待超时 timeout={}s nodeId={}", label, timeoutSec, nodeId);
        String targetNodeId = resolveSpecialTimeoutTarget(config);
        Map<String, Object> timeoutOutput = new LinkedHashMap<>();
        timeoutOutput.put(MapFieldKeys.NODE_ID, nodeId);
        timeoutOutput.put(MapFieldKeys.NODE_TYPE, node.getType());
        timeoutOutput.put(MapFieldKeys.OUTCOME, NodeOutcome.TIMEOUT.name());
        timeoutOutput.put(MapFieldKeys.REASON_CODE, "SPECIAL_NODE_TIMEOUT");
        timeoutOutput.put(MapFieldKeys.REASON_MESSAGE, label + " 等待超时");
        ctx.putNodeOutput(nodeId, timeoutOutput);
        writeTraceEnd(ctx, node, NodeResult.timeout(targetNodeId,
                "SPECIAL_NODE_TIMEOUT", label + " 等待超时"), 0);
        saveNodeStateSafely(ctx, nodeId, NodeStatus.TIMEOUT, timeoutOutput);

        if (targetNodeId == null || targetNodeId.isBlank()) {
            ctxStore.delete(ctx.getCanvasId(), ctx.getUserId());
            executionService.completePausedExecution(ctx, ExecutionStatus.FAILED.getCode(), timeoutOutput)
                    .subscribe(null,
                            e -> log.error("[{}] 超时终态落库失败 nodeId={}: {}", label, nodeId, e.getMessage()));
            return;
        }

        ctxStore.save(ctx);
        executeNode(graph, targetNodeId, ctx, depth + 1)
                .defaultIfEmpty(Map.of())
                .flatMap(result -> completeSpecialTimeoutContinuation(graph, ctx, result))
                .onErrorResume(e -> {
                    Map<String, Object> error = Map.of(
                            MapFieldKeys.ERROR, e.getMessage(),
                            MapFieldKeys.NODE_ID, nodeId,
                            MapFieldKeys.OUTCOME, NodeOutcome.TIMEOUT.name());
                    ctxStore.delete(ctx.getCanvasId(), ctx.getUserId());
                    return executionService.completePausedExecution(ctx,
                            ExecutionStatus.FAILED.getCode(), error);
                })
                .subscribe(null,
                        e -> log.error("[{}] 超时分支执行失败 nodeId={}: {}", label, nodeId, e.getMessage()));
    }

    private Mono<Void> completeSpecialTimeoutContinuation(
            DagGraph graph, ExecutionContext ctx, Map<String, Object> result) {
        Map<String, Object> safeResult = result == null ? Map.of() : result;
        if (hasWaitingNodes(ctx)) {
            ctxStore.save(ctx);
            return executionService.completePausedExecution(ctx,
                    ExecutionStatus.PAUSED.getCode(), safeResult);
        }
        writeSkippedNodesIfComplete(graph, ctx);
        ctxStore.delete(ctx.getCanvasId(), ctx.getUserId());
        return executionService.completePausedExecution(ctx,
                ExecutionStatus.SUCCESS.getCode(), safeResult);
    }

    private String resolveSpecialTimeoutTarget(Map<String, Object> config) {
        Object timeoutTarget = config.get(MapFieldKeys.TIMEOUT_NODE_ID);
        if (timeoutTarget instanceof String s && !s.isBlank()) {
            return s;
        }
        Object failTarget = config.get(MapFieldKeys.FAIL_NODE_ID);
        return failTarget instanceof String s && !s.isBlank() ? s : null;
    }

    /**
     * 执行 execute Node After Stage2 对应的业务逻辑。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param graph graph 方法执行所需的业务参数
     * @param nodeId nodeId 对应的业务主键或标识
     * @param node node 节点相关对象、标识或配置
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @param depth depth 方法执行所需的业务参数
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
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
                    // 锁已由 executeHandlerWithRepeat 统一释放，此处只处理状态和 trace。
                    if (!result.success()) {
                        ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                        long durationMs = System.currentTimeMillis() - nodeStartMs;
                        writeTraceEnd(ctx, node, result, durationMs);
                        saveNodeStateSafely(ctx, nodeId, NodeStatus.FAILED, result.output());
                        if (ctx.isBenefitGranted() || ctx.isUserReached()) return Mono.just(Map.of());
                        return triggerFailureAwareDownstream(graph, nodeId, node.getType(), ctx, depth,
                                result.errorMessage());
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
                        saveNodeStateSafely(ctx, nodeId, status, result.output());
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
                    saveNodeStateSafely(ctx, nodeId, status, result.output());
                    log.debug("[ENGINE] 节点完成 nodeId={} type={}", nodeId, node.getType());
                    return triggerDownstream(graph, result, nodeId, node.getType(), ctx, depth);
                })
                .onErrorResume(e -> {
                    // 异常时锁可能仍被持有（handler 内部抛出），在此统一释放
                    nodeGate.executing.set(false);
                    ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                    saveNodeStateSafely(ctx, nodeId, NodeStatus.FAILED, Map.of());
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
            // 叶子节点直接返回自身输出，作为本次 DAG 执行响应的一部分。
            return Mono.just(result.output() != null ? result.output() : Map.of());
        }
        prepareControlFlowReentry(graph, result, sourceNodeId, sourceType, nextIds, ctx);

        return Flux.fromIterable(nextIds)
                // 下游分支并行推进；汇聚节点依赖 NodeGate/repeat 保证单 execution 内不漏信号。
                .flatMap(nextId -> executeNode(graph, nextId, ctx, depth + 1))
                .last(Map.of());
    }

    /**
     * 当上游失败时，仅将失败信号继续传递给显式汇聚/判定节点。
     *
     * <p>普通业务节点仍然保持失败即中断，避免副作用链路被意外放开。
     */
    private Mono<Map<String, Object>> triggerFailureAwareDownstream(DagGraph graph,
                                                                     String sourceNodeId,
                                                                     String sourceType,
                                                                     ExecutionContext ctx,
                                                                     int depth,
                                                                     String errorMessage) {
        List<String> nextIds = graph.downstream(sourceNodeId).stream()
                .filter(nextId -> {
                    DagParser.CanvasNode nextNode = graph.getNode(nextId);
                    return nextNode != null && isFailureAwareConvergenceNode(nextNode.getType());
                })
                .distinct()
                .toList();

        if (nextIds.isEmpty()) {
            return Mono.error(new RuntimeException("节点 " + sourceNodeId + " 失败: " + errorMessage));
        }

        log.debug("[ENGINE] 失败信号继续传递到汇聚节点 sourceNodeId={} sourceType={} downstream={}",
                sourceNodeId, sourceType, nextIds);
        return Flux.fromIterable(nextIds)
                .flatMap(nextId -> executeNode(graph, nextId, ctx, depth + 1))
                .last(Map.of());
    }

    /**
     * 失败信号可继续传递的汇聚节点类型。
     */
    private boolean isFailureAwareConvergenceNode(String nodeType) {
        return NodeType.HUB.equals(nodeType)
                || NodeType.AGGREGATE.equals(nodeType)
                || NodeType.LOGIC_RELATION.equals(nodeType)
                || NodeType.THRESHOLD.equals(nodeType);
    }

    /**
     * 执行 prepare Control Flow Reentry 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param graph graph 方法执行所需的业务参数
     * @param result result 方法执行所需的业务参数
     * @param sourceNodeId sourceNodeId 对应的业务主键或标识
     * @param sourceType sourceType 类型标识或分类条件
     * @param nextIds nextIds 方法执行所需的业务参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     */
    private void prepareControlFlowReentry(DagGraph graph, NodeResult result, String sourceNodeId,
                                           String sourceType, List<String> nextIds, ExecutionContext ctx) {
        boolean looping = NodeType.LOOP.equals(sourceType)
                && result.routes() != null
                && result.routes().containsKey("loop");
        boolean jumping = NodeType.GOTO.equals(sourceType)
                && result.routes() != null
                && result.routes().containsKey("goto");
        if (!looping && !jumping) {
            return;
        }
        for (String nextId : nextIds) {
            // LOOP/GOTO 回流到旧路径前清理可达节点状态，使下一轮能重新通过幂等检查。
            resetReachableUntilSource(graph, nextId, sourceNodeId, ctx);
        }
    }

    /**
     * 执行 reset Reachable Until Source 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param graph graph 方法执行所需的业务参数
     * @param startNodeId startNodeId 对应的业务主键或标识
     * @param sourceNodeId sourceNodeId 对应的业务主键或标识
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     */
    private void resetReachableUntilSource(DagGraph graph, String startNodeId, String sourceNodeId,
                                           ExecutionContext ctx) {
        Deque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(startNodeId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            ctx.resetNodeStatusForReentry(current);
            if (current.equals(sourceNodeId)) {
                continue;
            }
            graph.downstream(current).forEach(queue::addLast);
        }
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
                saveNodeStateSafely(ctx, priorityNodeId, NodeStatus.PARTIAL_FAIL, Map.of());
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
        List<CanvasExecutionTraceDO> skippedTraces = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        graph.getNodeMap().forEach((nodeId, node) -> {
            if (ctx.setNodeStatusIfAbsent(nodeId, NodeStatus.SKIPPED)) {
                // 只为从未进入状态机的节点补 SKIPPED，避免覆盖已执行/等待/失败节点。
                skippedTraces.add(CanvasExecutionTraceDO.builder()
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

    /**
     * 写入或记录 write Skipped Nodes If Complete 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param graph graph 方法执行所需的业务参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     */
    private void writeSkippedNodesIfComplete(DagGraph graph, ExecutionContext ctx) {
        if (hasWaitingNodes(ctx)) {
            log.debug("[ENGINE] 执行已挂起，暂不批量写入 SKIPPED executionId={}", ctx.getExecutionId());
            return;
        }
        writeSkippedNodes(graph, ctx);
    }

    /**
     * 判断 has Waiting Nodes 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean hasWaitingNodes(ExecutionContext ctx) {
        return ctx.getNodeStatuses().values().stream().anyMatch(status -> status == NodeStatus.WAITING);
    }

    // ══════════════════════════════════════════════════════════════
    // ══════════════════════════════════════════════════════════════
    // 执行轨迹写入（通过 TraceWriteBuffer 异步批量写，12.10节）
    // ══════════════════════════════════════════════════════════════

    private void writeTraceStart(ExecutionContext ctx, DagParser.CanvasNode node) {
        CanvasExecutionTraceDO trace = CanvasExecutionTraceDO.builder()
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
                // trace 只保存节点输出快照，序列化失败不阻断业务结果返回。
                outputJson = objectMapper.writeValueAsString(DataMaskingUtil.maskObject(result.output()));
            }
        } catch (Exception ignored) {
        }

        CanvasExecutionTraceDO trace = CanvasExecutionTraceDO.builder()
                .executionId(ctx.getExecutionId())
                .nodeId(node.getId())
                .nodeType(node.getType())
                .nodeName(node.getName())
                .status(status)
                .outputData(outputJson)
                .errorMsg(DataMaskingUtil.maskText(result.errorMessage()))
                .finishedAt(LocalDateTime.now())
                .durationMs(durationMs > 0 ? durationMs : null)
                .build();
        traceBuffer.offer(trace);
    }

    /**
     * 写入或记录 write Trace End 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @param node node 节点相关对象、标识或配置
     * @param result result 方法执行所需的业务参数
     */
    private void writeTraceEnd(ExecutionContext ctx, DagParser.CanvasNode node, NodeResult result) {
        writeTraceEnd(ctx, node, result, 0);
    }

    /**
     * 执行 collect Next Ids 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param result result 方法执行所需的业务参数
     * @return 查询、转换或计算得到的结果集合
     */
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

    /**
     * 执行 status For Outcome 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param outcome outcome 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
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

    private void saveNodeStateSafely(ExecutionContext ctx, String nodeId,
                                     NodeStatus status, Map<String, Object> output) {
        try {
            ctxStore.saveNodeState(ctx.getExecutionId(), nodeId, status,
                    output == null ? Map.of() : output);
        } catch (Exception e) {
            log.warn("[ENGINE] 节点增量状态持久化失败 executionId={} nodeId={} status={}: {}",
                    ctx.getExecutionId(), nodeId, status, e.getMessage());
        }
    }

    /**
     * 执行 trace Status 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param result result 方法执行所需的业务参数
     * @return 计算得到的数值结果
     */
    private int traceStatus(NodeResult result) {
        if (!result.success()) {
            return 2;
        }
        if (result.outcome() == NodeOutcome.TIMEOUT || result.outcome() == NodeOutcome.FAIL) {
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

    /**
     * 执行 pending Response 对应的业务逻辑。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param nodeId nodeId 对应的业务主键或标识
     * @param nodeType nodeType 节点相关对象、标识或配置
     * @param result result 方法执行所需的业务参数
     * @return 按业务键组织的映射结果
     */
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

    /**
     * 执行 enrich Wait Resume Payload 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param resolved resolved 方法执行所需的业务参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @param nodeId nodeId 对应的业务主键或标识
     * @param nodeType nodeType 节点相关对象、标识或配置
     */
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
            markSkippedPath(graph, skippedId, ctx);

        } else if ("SELECTOR".equals(sourceType)) {
            // 标记所有未走的 branch 入口
            java.util.List<Map<String, Object>> branches =
                    (java.util.List<Map<String, Object>>) cfg.getOrDefault(MapFieldKeys.BRANCHES, List.of());
            String elseId = (String) cfg.get(MapFieldKeys.ELSE_NODE_ID);
            String takenId = result.nextNodeId(); // SELECTOR 走的那条

            branches.forEach(b -> {
                String branchNext = (String) b.get(MapFieldKeys.NEXT_NODE_ID);
                if (branchNext != null && !branchNext.equals(takenId)) {
                    markSkippedPath(graph, branchNext, ctx);
                }
            });
            if (elseId != null && !elseId.equals(takenId)) {
                markSkippedPath(graph, elseId, ctx);
            }
        }
    }

    /**
     * 写入或记录 mark Skipped Path 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param graph graph 方法执行所需的业务参数
     * @param nodeId nodeId 对应的业务主键或标识
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     */
    private void markSkippedPath(DagGraph graph, String nodeId, ExecutionContext ctx) {
        if (nodeId == null || nodeId.isBlank()) {
            return;
        }
        Deque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(nodeId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            if (ctx.setNodeStatusIfNotDone(current, NodeStatus.SKIPPED)) {
                log.debug("[ENGINE] 立即标记 SKIPPED nodeId={}", current);
                saveNodeStateSafely(ctx, current, NodeStatus.SKIPPED, Map.of());
            }
            for (String downstream : graph.downstream(current)) {
                if (allUpstreamSkipped(graph, downstream, ctx)) {
                    queue.addLast(downstream);
                }
            }
        }
    }

    /**
     * 执行 all Upstream Skipped 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param graph graph 方法执行所需的业务参数
     * @param nodeId nodeId 对应的业务主键或标识
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean allUpstreamSkipped(DagGraph graph, String nodeId, ExecutionContext ctx) {
        List<String> upstreamIds = graph.upstream(nodeId);
        return !upstreamIds.isEmpty()
                && upstreamIds.stream().allMatch(upstream -> ctx.getNodeStatus(upstream) == NodeStatus.SKIPPED);
    }
}
