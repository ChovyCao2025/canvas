package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.chovy.canvas.common.DataMaskingUtil;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeGate;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handler.HandlerRegistry;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.handlers.HubHandler;
import org.chovy.canvas.engine.idempotency.NodeSideEffectIdempotencyService;
import org.chovy.canvas.engine.trace.ExecutionTraceContext;
import org.chovy.canvas.infrastructure.reactor.TrackedReactiveTaskRegistry;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.infrastructure.redis.RedisDelayQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 4. Hub 超时延迟任务（设计文档 Hub 定义）<br>
 * 5. Priority 串行依序尝试（4.6节）<br>
 * 6. 执行结束批量写入 SKIPPED（7.7节）
 */
@Slf4j
@Component
public class DagEngine {

    /** 节点处理器注册表。 */
    private final HandlerRegistry handlerRegistry;
    /** 执行轨迹异步写入缓冲区。 */
    private final TraceWriteBuffer traceBuffer;
    /** 画布执行死信写入器。 */
    private final ExecutionDlqWriter dlqWriter;
    /** 节点执行结果路由器。 */
    private final NodeResultRouter nodeResultRouter;
    /** 节点执行门控协调器。 */
    private final NodeGateCoordinator nodeGateCoordinator;
    /** 特殊等待节点超时调度协调器。 */
    private final NodeTimeoutCoordinator nodeTimeoutCoordinator;
    /** 跨实例可恢复的特殊等待节点超时延时队列。 */
    private final RedisDelayQueue delayQueue;
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
    /** 特殊节点等待超时调度器。 */
    private final Scheduler specialNodeTimeoutScheduler;
    /** 当前 DagEngine 是否拥有特殊节点超时调度器的关闭权。 */
    private final boolean ownsSpecialNodeTimeoutScheduler;
    /** 跟踪特殊节点超时和续跑任务，避免引擎内部 fire-and-forget 失去生命周期管理。 */
    private final TrackedReactiveTaskRegistry reactiveTaskRegistry;
    /** 节点副作用幂等服务；测试构造器可为空，生产由 Spring 可选注入。 */
    private NodeSideEffectIdempotencyService sideEffectIdempotencyService;

    /**
     * 构造 DagEngine 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param handlerRegistry handlerRegistry 方法执行所需的业务参数
     * @param traceBuffer traceBuffer 方法执行所需的业务参数
     * @param cbRegistry cbRegistry 方法执行所需的业务参数
     * @param metrics metrics 方法执行所需的业务参数
     * @param objectMapper objectMapper 方法执行所需的业务参数
     * @param ctxStore ctxStore 方法执行所需的业务参数
     * @param executionService executionService 方法执行所需的业务参数
     */
    @Autowired
    public DagEngine(HandlerRegistry handlerRegistry,
                     TraceWriteBuffer traceBuffer,
                     CircuitBreakerRegistry cbRegistry,
                     CanvasMetrics metrics,
                     ObjectMapper objectMapper,
                     ContextPersistenceService ctxStore,
                     @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService,
                     TrackedReactiveTaskRegistry reactiveTaskRegistry,
                     ExecutionDlqWriter dlqWriter,
                     NodeResultRouter nodeResultRouter,
                     NodeGateCoordinator nodeGateCoordinator,
                     NodeTimeoutCoordinator nodeTimeoutCoordinator,
                     RedisDelayQueue delayQueue) {
        this(handlerRegistry, traceBuffer, cbRegistry, metrics, objectMapper, ctxStore,
                executionService, newSpecialNodeTimeoutScheduler(), true, reactiveTaskRegistry, dlqWriter,
                nodeResultRouter, nodeGateCoordinator, nodeTimeoutCoordinator, delayQueue);
    }

    DagEngine(HandlerRegistry handlerRegistry,
              TraceWriteBuffer traceBuffer,
              CircuitBreakerRegistry cbRegistry,
              CanvasMetrics metrics,
              ObjectMapper objectMapper,
              ContextPersistenceService ctxStore,
              @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService,
              ExecutionDlqWriter dlqWriter) {
        this(handlerRegistry, traceBuffer, cbRegistry, metrics, objectMapper, ctxStore,
                executionService, dlqWriter, new NodeResultRouter(), new NodeGateCoordinator(),
                new NodeTimeoutCoordinator());
    }

    DagEngine(HandlerRegistry handlerRegistry,
              TraceWriteBuffer traceBuffer,
              CircuitBreakerRegistry cbRegistry,
              CanvasMetrics metrics,
              ObjectMapper objectMapper,
              ContextPersistenceService ctxStore,
              @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService,
              ExecutionDlqWriter dlqWriter,
              NodeResultRouter nodeResultRouter) {
        this(handlerRegistry, traceBuffer, cbRegistry, metrics, objectMapper, ctxStore,
                executionService, dlqWriter, nodeResultRouter, new NodeGateCoordinator(),
                new NodeTimeoutCoordinator());
    }

    DagEngine(HandlerRegistry handlerRegistry,
              TraceWriteBuffer traceBuffer,
              CircuitBreakerRegistry cbRegistry,
              CanvasMetrics metrics,
              ObjectMapper objectMapper,
              ContextPersistenceService ctxStore,
              @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService,
              ExecutionDlqWriter dlqWriter,
              NodeResultRouter nodeResultRouter,
              NodeGateCoordinator nodeGateCoordinator) {
        this(handlerRegistry, traceBuffer, cbRegistry, metrics, objectMapper, ctxStore,
                executionService, dlqWriter, nodeResultRouter, nodeGateCoordinator,
                new NodeTimeoutCoordinator());
    }

    DagEngine(HandlerRegistry handlerRegistry,
              TraceWriteBuffer traceBuffer,
              CircuitBreakerRegistry cbRegistry,
              CanvasMetrics metrics,
              ObjectMapper objectMapper,
              ContextPersistenceService ctxStore,
              @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService,
              ExecutionDlqWriter dlqWriter,
              NodeResultRouter nodeResultRouter,
              NodeGateCoordinator nodeGateCoordinator,
              NodeTimeoutCoordinator nodeTimeoutCoordinator) {
        this(handlerRegistry, traceBuffer, cbRegistry, metrics, objectMapper, ctxStore,
                executionService, Schedulers.parallel(), false, TrackedReactiveTaskRegistry.direct(), dlqWriter,
                nodeResultRouter, nodeGateCoordinator, nodeTimeoutCoordinator, null);
    }

    DagEngine(HandlerRegistry handlerRegistry,
              TraceWriteBuffer traceBuffer,
              CircuitBreakerRegistry cbRegistry,
              CanvasMetrics metrics,
              ObjectMapper objectMapper,
              ContextPersistenceService ctxStore,
              @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService,
              ExecutionDlqWriter dlqWriter,
              NodeResultRouter nodeResultRouter,
              NodeGateCoordinator nodeGateCoordinator,
              NodeTimeoutCoordinator nodeTimeoutCoordinator,
              RedisDelayQueue delayQueue) {
        this(handlerRegistry, traceBuffer, cbRegistry, metrics, objectMapper, ctxStore,
                executionService, Schedulers.parallel(), false, TrackedReactiveTaskRegistry.direct(), dlqWriter,
                nodeResultRouter, nodeGateCoordinator, nodeTimeoutCoordinator, delayQueue);
    }

    DagEngine(HandlerRegistry handlerRegistry,
              TraceWriteBuffer traceBuffer,
              CircuitBreakerRegistry cbRegistry,
              CanvasMetrics metrics,
              ObjectMapper objectMapper,
              ContextPersistenceService ctxStore,
              @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService,
              Scheduler specialNodeTimeoutScheduler,
              boolean ownsSpecialNodeTimeoutScheduler,
              ExecutionDlqWriter dlqWriter) {
        this(handlerRegistry, traceBuffer, cbRegistry, metrics, objectMapper, ctxStore,
                executionService, specialNodeTimeoutScheduler, ownsSpecialNodeTimeoutScheduler,
                dlqWriter, new NodeResultRouter(), new NodeGateCoordinator(), new NodeTimeoutCoordinator());
    }

    DagEngine(HandlerRegistry handlerRegistry,
              TraceWriteBuffer traceBuffer,
              CircuitBreakerRegistry cbRegistry,
              CanvasMetrics metrics,
              ObjectMapper objectMapper,
              ContextPersistenceService ctxStore,
              @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService,
              Scheduler specialNodeTimeoutScheduler,
              boolean ownsSpecialNodeTimeoutScheduler,
              ExecutionDlqWriter dlqWriter,
              NodeResultRouter nodeResultRouter) {
        this(handlerRegistry, traceBuffer, cbRegistry, metrics, objectMapper, ctxStore,
                executionService, specialNodeTimeoutScheduler, ownsSpecialNodeTimeoutScheduler,
                dlqWriter, nodeResultRouter, new NodeGateCoordinator(), new NodeTimeoutCoordinator());
    }

    DagEngine(HandlerRegistry handlerRegistry,
              TraceWriteBuffer traceBuffer,
              CircuitBreakerRegistry cbRegistry,
              CanvasMetrics metrics,
              ObjectMapper objectMapper,
              ContextPersistenceService ctxStore,
              @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService,
              Scheduler specialNodeTimeoutScheduler,
              boolean ownsSpecialNodeTimeoutScheduler,
              ExecutionDlqWriter dlqWriter,
              NodeResultRouter nodeResultRouter,
              NodeGateCoordinator nodeGateCoordinator) {
        this(handlerRegistry, traceBuffer, cbRegistry, metrics, objectMapper, ctxStore,
                executionService, specialNodeTimeoutScheduler, ownsSpecialNodeTimeoutScheduler,
                dlqWriter, nodeResultRouter, nodeGateCoordinator, new NodeTimeoutCoordinator());
    }

    DagEngine(HandlerRegistry handlerRegistry,
              TraceWriteBuffer traceBuffer,
              CircuitBreakerRegistry cbRegistry,
              CanvasMetrics metrics,
              ObjectMapper objectMapper,
              ContextPersistenceService ctxStore,
              @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService,
              Scheduler specialNodeTimeoutScheduler,
              boolean ownsSpecialNodeTimeoutScheduler,
              ExecutionDlqWriter dlqWriter,
              NodeResultRouter nodeResultRouter,
              NodeGateCoordinator nodeGateCoordinator,
              NodeTimeoutCoordinator nodeTimeoutCoordinator) {
        this(handlerRegistry, traceBuffer, cbRegistry, metrics, objectMapper, ctxStore,
                executionService, specialNodeTimeoutScheduler, ownsSpecialNodeTimeoutScheduler,
                TrackedReactiveTaskRegistry.direct(), dlqWriter, nodeResultRouter, nodeGateCoordinator,
                nodeTimeoutCoordinator, null);
    }

    DagEngine(HandlerRegistry handlerRegistry,
              TraceWriteBuffer traceBuffer,
              CircuitBreakerRegistry cbRegistry,
              CanvasMetrics metrics,
              ObjectMapper objectMapper,
              ContextPersistenceService ctxStore,
              @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService,
              Scheduler specialNodeTimeoutScheduler,
              boolean ownsSpecialNodeTimeoutScheduler,
              TrackedReactiveTaskRegistry reactiveTaskRegistry,
              ExecutionDlqWriter dlqWriter,
              NodeResultRouter nodeResultRouter,
              NodeGateCoordinator nodeGateCoordinator,
              NodeTimeoutCoordinator nodeTimeoutCoordinator) {
        this(handlerRegistry, traceBuffer, cbRegistry, metrics, objectMapper, ctxStore,
                executionService, specialNodeTimeoutScheduler, ownsSpecialNodeTimeoutScheduler,
                reactiveTaskRegistry, dlqWriter, nodeResultRouter, nodeGateCoordinator,
                nodeTimeoutCoordinator, null);
    }

    DagEngine(HandlerRegistry handlerRegistry,
              TraceWriteBuffer traceBuffer,
              CircuitBreakerRegistry cbRegistry,
              CanvasMetrics metrics,
              ObjectMapper objectMapper,
              ContextPersistenceService ctxStore,
              @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService,
              Scheduler specialNodeTimeoutScheduler,
              boolean ownsSpecialNodeTimeoutScheduler,
              TrackedReactiveTaskRegistry reactiveTaskRegistry,
              ExecutionDlqWriter dlqWriter,
              NodeResultRouter nodeResultRouter,
              NodeGateCoordinator nodeGateCoordinator,
              NodeTimeoutCoordinator nodeTimeoutCoordinator,
              RedisDelayQueue delayQueue) {
        this.handlerRegistry = handlerRegistry;
        // traceMapper 保留供 writeSkippedNodes 直接写入；批量写走 traceBuffer
        this.traceBuffer = traceBuffer;
        this.dlqWriter = dlqWriter;
        this.nodeResultRouter = nodeResultRouter;
        this.nodeGateCoordinator = nodeGateCoordinator;
        this.nodeTimeoutCoordinator = nodeTimeoutCoordinator;
        this.delayQueue = delayQueue;
        this.cbRegistry = cbRegistry;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.ctxStore = ctxStore;
        this.executionService = executionService;
        this.specialNodeTimeoutScheduler = specialNodeTimeoutScheduler;
        this.ownsSpecialNodeTimeoutScheduler = ownsSpecialNodeTimeoutScheduler;
        this.reactiveTaskRegistry = reactiveTaskRegistry;
    }

    @Autowired(required = false)
    void setSideEffectIdempotencyService(NodeSideEffectIdempotencyService sideEffectIdempotencyService) {
        this.sideEffectIdempotencyService = sideEffectIdempotencyService;
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
    private static Scheduler newSpecialNodeTimeoutScheduler() {
        return Schedulers.newBoundedElastic(16, 10_000, "canvas-special-node-timeout", 60, true);
    }

    /** 关闭特殊节点等待超时调度器，避免应用停止后保留后台调度线程。 */
    @PreDestroy
    void shutdownSpecialNodeTimeoutScheduler() {
        if (ownsSpecialNodeTimeoutScheduler) {
            specialNodeTimeoutScheduler.dispose();
        }
    }

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

            boolean needsNodeId = NodeType.API_CALL.equals(node.getType())
                    || NodeType.WAIT.equals(node.getType())
                    || NodeType.USER_INPUT.equals(node.getType())
                    || NodeType.SEND_MESSAGE.equals(node.getType())
                    || NodeType.COMMIT_ACTION.equals(node.getType())
                    || NodeType.TAGGER.equals(node.getType());
            Map<String, Object> config = needsNodeId
                    ? resolveConfigWithNodeId(rawConfig, ctx, nodeId, node.getType())
                    : resolveConfig(rawConfig, ctx);

            Mono<Map<String, Object>> specialTimeoutTrigger =
                    executeSpecialTimeoutTriggerIfNeeded(graph, nodeId, node, config, ctx, depth);
            if (specialTimeoutTrigger != null) {
                return specialTimeoutTrigger;
            }

            // ──────────────────────────────────────────────────────
            // 阶段 2：HUB / AGGREGATE / THRESHOLD 特殊处理
            // ──────────────────────────────────────────────────────
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
            if (!nodeGateCoordinator.tryAcquireOrSignalRepeat(nodeGate)) {
                // 抢锁失败：set(true) 向持锁协程发送 repeat 信号
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
                        if (shouldCommitOutput(result)) {
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
                        nodeGateCoordinator.release(nodeGate); // 释放异常锁
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
     * <h3>设计假设：普通节点在同一 execution 内只有一条活跃的上游 trigger</h3>
     * <pre>
     * repeat 机制的保护对象是"最后到达的信号不被漏掉"，它保证下游只触发一次、状态只写一次。
     * 但 handler 本身（executeAsync）在发生 CAS 竞争时会被调用两次：
     *   第一次：CAS 胜者持锁执行
     *   第二次：胜者释放锁后，因 repeatPending=true 重入执行
     *
     * 对于 HUB/AGGREGATE：handler 是纯路由逻辑，无副作用，两次调用无害。
     * 对于 THRESHOLD：两次调用是语义必要的（第二次才能看到全部上游状态）。
     * 对于普通节点（发消息、发券、调 API 等）：若两条并发 trigger 同时到达且 handler
     *   执行期间存在竞争，handler 的副作用会执行两次。
     *
     * 因此，Canvas DAG 的设计约束是：
     *   多分支收敛必须经由显式的收敛节点（HUB / AGGREGATE / THRESHOLD）。
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
                        return Mono.just(NodeResult.fail(e.getMessage()));
                    }
                    ExecutionTraceContext traceContext = ExecutionTraceContext.from(ctx, nodeId);
                    try (ExecutionTraceContext.Scope ignored = traceContext.open()) {
                        return traceContext.scope(executeWithSideEffectIdempotency(
                                handler, config, ctx, nodeId, nodeType)); // ← handler 真正在这里被调用
                    }
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
            dlqWriter.write(ctx, nodeId, nodeType, e, maxRetry);
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
                nodeGateCoordinator.release(nodeGate);
                return Mono.just(result);
            }

            // 先读并清除 repeatPending，再释放 executing 锁。
            // 顺序关键：必须在释放锁之前读信号，否则新协程抢锁后才来的请求会被遗漏。
            // （详见 executeHandlerWithRepeat JavaDoc 中的 Case 3 分析）
            boolean needsRepeat = nodeGateCoordinator.releaseAndConsumeRepeat(nodeGate);

            if (needsRepeat && nodeGateCoordinator.tryAcquireForRepeat(nodeGate)) {
                // 重新持锁后返回 withRetry.doFinally(__)：
                //   flatMap 订阅它 → handler 第二次执行（repeat）
                //   doFinally → repeat 结束后释放锁（不管成功/失败/取消）
                log.debug("[ENGINE] repeat nodeId={}", nodeId);
                return withRetry.doFinally(__ -> nodeGateCoordinator.release(nodeGate));
            }

            // needsRepeat=false，或 CAS 失败（另一协程已接管）：不 repeat
            return Mono.just(result);
        });
    }

    private Mono<NodeResult> executeWithSideEffectIdempotency(NodeHandler handler,
                                                             Map<String, Object> config,
                                                             ExecutionContext ctx,
                                                             String nodeId,
                                                             String nodeType) {
        if (sideEffectIdempotencyService == null
                || !handler.requiresSideEffectIdempotency(config, ctx)) {
            return handler.executeAsync(config, ctx);
        }

        NodeSideEffectIdempotencyService.ReserveResult reservation;
        try {
            reservation = sideEffectIdempotencyService.reserve(
                    ctx, nodeId, nodeType, handler.sideEffectOperationKey(config, ctx));
        } catch (Exception e) {
            log.error("[ENGINE] 副作用幂等预留失败 nodeId={} type={}: {}", nodeId, nodeType, e.getMessage(), e);
            return Mono.just(NodeResult.fail("SIDE_EFFECT_IDEMPOTENCY: " + e.getMessage()));
        }

        if (reservation.completed()) {
            log.debug("[ENGINE] 副作用幂等命中缓存 nodeId={} type={}", nodeId, nodeType);
            return Mono.just(handler.completedSideEffectResult(config, ctx, reservation.cachedOutput()));
        }
        if (reservation.exhausted()) {
            return Mono.just(NodeResult.fail("SIDE_EFFECT_IDEMPOTENCY_EXHAUSTED: max attempts reached"));
        }

        Long recordId = reservation.record() == null ? null : reservation.record().getId();
        return handler.executeAsync(config, ctx)
                .doOnNext(result -> {
                    if (recordId == null) {
                        return;
                    }
                    if (result.success() && !result.pending()) {
                        sideEffectIdempotencyService.complete(recordId, result.output());
                    } else if (!result.success()) {
                        sideEffectIdempotencyService.fail(recordId, result.errorMessage());
                    }
                })
                .doOnError(e -> {
                    if (recordId != null) {
                        sideEffectIdempotencyService.fail(recordId, e.getMessage());
                    }
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
        if (status == NodeStatus.FAILED || status == NodeStatus.TIMEOUT) {
            // 特殊汇聚节点进入失败类终态后不再恢复执行，防止超时恢复重复改写路由结果。
            return Mono.error(new RuntimeException("节点 " + nodeId + " 已处于终态: " + status));
        }
        if (ctx.isNodeDone(nodeId)) {
            // 已完成的 special 节点按幂等返回，避免恢复触发重复写 trace 或重复下游路由。
            return Mono.just(Map.of());
        }
        return null;
    }

    /** 特殊等待节点的全局超时秒数兜底。 */
    @org.springframework.beans.factory.annotation.Value("${canvas.execution.global-timeout-sec:600}")
    private long globalTimeout;

    private Mono<Map<String, Object>> executeSpecialTimeoutTriggerIfNeeded(
            DagGraph graph,
            String nodeId,
            DagParser.CanvasNode node,
            Map<String, Object> config,
            ExecutionContext ctx,
            int depth) {
        if (!RedisDelayQueue.isSpecialTimeoutTrigger(ctx.getTriggerType())) {
            return null;
        }
        if (!isSpecialTimeoutNode(node.getType())
                || !Objects.equals(ctx.getTriggerNodeType(), node.getType())
                || !Objects.equals(ctx.getMatchKey(), nodeId)) {
            return null;
        }
        String timerKey = RedisDelayQueue.timerKey(node.getType(), nodeId);
        if (!timeoutPayloadMatches(ctx, nodeId, timerKey)) {
            return Mono.just(Map.of(MapFieldKeys.SKIPPED, "stale-timeout-payload"));
        }
        int timeoutSec = timeoutSecondsForSpecialNode(node.getType(), config, ctx);
        return executeSpecialTimeoutBranch(graph, nodeId, node, config, ctx, depth,
                specialNodeLabel(node.getType()), timeoutSec);
    }

    private boolean timeoutPayloadMatches(ExecutionContext ctx, String nodeId, String timerKey) {
        if (ctx.getNodeStatus(nodeId) != NodeStatus.WAITING) {
            return false;
        }
        Map<String, Object> payload = ctx.getTriggerPayload();
        Object payloadExecutionId = payload.get(MapFieldKeys.EXECUTION_ID);
        if (payloadExecutionId != null && !Objects.equals(ctx.getExecutionId(), payloadExecutionId.toString())) {
            return false;
        }
        Long payloadVersionId = payloadLong(payload.get(MapFieldKeys.VERSION_ID));
        if (payloadVersionId != null && !Objects.equals(ctx.getVersionId(), payloadVersionId)) {
            return false;
        }
        Object payloadTimerKey = payload.get(MapFieldKeys.TIMEOUT_TIMER_KEY);
        if (payloadTimerKey == null || !Objects.equals(timerKey, payloadTimerKey.toString())) {
            return false;
        }
        Long payloadScheduledAt = payloadLong(payload.get(MapFieldKeys.TIMEOUT_SCHEDULED_AT_EPOCH_MS));
        Long currentScheduledAt = ctx.getHubStartTimes().get(timerKey);
        return payloadScheduledAt != null && Objects.equals(currentScheduledAt, payloadScheduledAt);
    }

    private Mono<Map<String, Object>> executeSpecialTimeoutBranch(
            DagGraph graph,
            String nodeId,
            DagParser.CanvasNode node,
            Map<String, Object> config,
            ExecutionContext ctx,
            int depth,
            String label,
            int timeoutSec) {
        if (!ctx.setNodeStatusIfNotDone(nodeId, NodeStatus.TIMEOUT)) {
            return Mono.just(Map.of(MapFieldKeys.SKIPPED, "timeout-node-terminal"));
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
        saveSpecialNodeState(ctx, nodeId, NodeStatus.TIMEOUT, timeoutOutput);
        writeTraceEnd(ctx, node, NodeResult.timeout(targetNodeId,
                "SPECIAL_NODE_TIMEOUT", label + " 等待超时"), 0);

        if (targetNodeId == null || targetNodeId.isBlank()) {
            return Mono.error(new RuntimeException(label + " 等待超时 nodeId=" + nodeId));
        }
        return executeNode(graph, targetNodeId, ctx, depth + 1).defaultIfEmpty(Map.of());
    }

    private boolean scheduleSpecialNodeTimeoutRequired(
            ExecutionContext ctx, String nodeId, String nodeType, int timeoutSec) {
        String timerKey = RedisDelayQueue.timerKey(nodeType, nodeId);
        if (!ctx.getScheduledHubTimeouts().add(timerKey)) {
            return false;
        }
        ctx.getHubStartTimes().putIfAbsent(timerKey, System.currentTimeMillis());
        try {
            delayQueue.scheduleSpecialNodeTimeout(ctx, nodeId, nodeType, timeoutSec);
            return true;
        } catch (Exception e) {
            ctx.getScheduledHubTimeouts().remove(timerKey);
            ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
            Map<String, Object> output = Map.of(
                    MapFieldKeys.ERROR, e.getMessage() == null ? "redis delay queue unavailable" : e.getMessage(),
                    MapFieldKeys.NODE_ID, nodeId);
            saveSpecialNodeState(ctx, nodeId, NodeStatus.FAILED, output);
            throw new SpecialNodeTimeoutFailureException(
                    "Failed to schedule special node timeout nodeId=" + nodeId, e);
        }
    }

    private void saveSpecialNodeState(
            ExecutionContext ctx, String nodeId, NodeStatus status, Map<String, Object> output) {
        if (ctx.getExecutionId() == null || ctx.getExecutionId().isBlank()) {
            return;
        }
        ctxStore.saveNodeState(ctx.getExecutionId(), nodeId, status, output == null ? Map.of() : output);
    }

    private void clearSpecialTimeoutGeneration(ExecutionContext ctx, String nodeId, String nodeType) {
        if (!isSpecialTimeoutNode(nodeType)) {
            return;
        }
        String timerKey = RedisDelayQueue.timerKey(nodeType, nodeId);
        ctx.getHubStartTimes().remove(timerKey);
        ctx.getScheduledHubTimeouts().remove(timerKey);
    }

    private boolean isSpecialTimeoutNode(String nodeType) {
        return NodeType.HUB.equals(nodeType)
                || NodeType.AGGREGATE.equals(nodeType)
                || NodeType.THRESHOLD.equals(nodeType);
    }

    private int timeoutSecondsForSpecialNode(String nodeType, Map<String, Object> config, ExecutionContext ctx) {
        Long payloadTimeout = payloadLong(ctx.getTriggerPayload().get(MapFieldKeys.TIMEOUT_SECONDS));
        if (payloadTimeout != null) {
            return payloadTimeout.intValue();
        }
        if (NodeType.HUB.equals(nodeType)) {
            return HubHandler.getTimeoutSeconds(config);
        }
        return config.get("timeout") instanceof Number n ? n.intValue() : (int) globalTimeout;
    }

    private String specialNodeLabel(String nodeType) {
        return switch (nodeType) {
            case NodeType.HUB -> "HUB";
            case NodeType.AGGREGATE -> "AGGREGATE";
            case NodeType.THRESHOLD -> "THRESHOLD";
            default -> nodeType;
        };
    }

    private Long payloadLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

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
        // ── 并发安全性说明 ───────────────
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
            ctx.setNodeStatusIfNotDone(nodeId, NodeStatus.WAITING);  // 设 WAITING 供 isPaused 检测
            int timeoutSec = HubHandler.getTimeoutSeconds(config);
            saveSpecialNodeState(ctx, nodeId, NodeStatus.WAITING, Map.of());
            if (delayQueue != null) {
                boolean scheduled = scheduleSpecialNodeTimeoutRequired(ctx, nodeId, NodeType.HUB, timeoutSec);
                if (scheduled) {
                    log.debug("[HUB] 启动 Redis 超时延迟队列 {}s nodeId={}", timeoutSec, nodeId);
                }
                return Mono.just(Map.of());
            }
            boolean scheduled = nodeTimeoutCoordinator.scheduleOnce(
                    ctx,
                    nodeId,
                    nodeId,
                    "dag-hub-timeout-" + ctx.getExecutionId() + "-" + nodeId,
                    timeoutSec,
                    specialNodeTimeoutScheduler,
                    reactiveTaskRegistry,
                    () -> handleSpecialNodeTimeout(graph, nodeId, node, config, ctx, depth,
                            "HUB", timeoutSec),
                    e -> log.error("[HUB] 超时定时器失败 nodeId={}: {}", nodeId, e.getMessage()));
            if (scheduled) {
                log.debug("[HUB] 启动超时定时器 {}s nodeId={}", timeoutSec, nodeId);
            } else {
                // 已调度过定时器，检查是否已超时
                if (nodeTimeoutCoordinator.hasElapsed(ctx, nodeId, timeoutSec)) {
                    ctx.setNodeStatusIfNotDone(nodeId, NodeStatus.FAILED);
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
            ctx.setNodeStatusIfNotDone(nodeId, NodeStatus.WAITING);
            String timerKey = "ag:" + nodeId;
            int timeoutSec = config.get("timeout") instanceof Number n ? n.intValue() : (int) globalTimeout;
            saveSpecialNodeState(ctx, nodeId, NodeStatus.WAITING, Map.of());
            if (delayQueue != null) {
                boolean scheduled = scheduleSpecialNodeTimeoutRequired(ctx, nodeId, NodeType.AGGREGATE, timeoutSec);
                if (scheduled) {
                    log.debug("[AGGREGATE] 启动 Redis 超时延迟队列 {}s nodeId={}", timeoutSec, nodeId);
                }
                return Mono.just(Map.of());
            }
            boolean scheduled = nodeTimeoutCoordinator.scheduleOnce(
                    ctx,
                    timerKey,
                    timerKey,
                    "dag-aggregate-timeout-" + ctx.getExecutionId() + "-" + nodeId,
                    timeoutSec,
                    specialNodeTimeoutScheduler,
                    reactiveTaskRegistry,
                    () -> handleSpecialNodeTimeout(graph, nodeId, node, config, ctx, depth,
                            "AGGREGATE", timeoutSec),
                    e -> log.error("[AGGREGATE] 超时定时器失败 nodeId={}: {}", nodeId, e.getMessage()));
            if (scheduled) {
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
    // 阶段 3-6 公共入口（HUB / AGGREGATE 满足条件后调用）
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
        int timeoutSec = config.get("timeout") instanceof Number n ? n.intValue() : (int) globalTimeout;
        if (delayQueue != null) {
            scheduleSpecialNodeTimeoutRequired(ctx, nodeId, NodeType.THRESHOLD, timeoutSec);
            return;
        }
        nodeTimeoutCoordinator.scheduleOnce(
                ctx,
                timerKey,
                timerKey,
                "dag-threshold-timeout-" + ctx.getExecutionId() + "-" + nodeId,
                timeoutSec,
                specialNodeTimeoutScheduler,
                reactiveTaskRegistry,
                () -> handleSpecialNodeTimeout(graph, nodeId, node, config, ctx, depth,
                        "THRESHOLD", timeoutSec),
                e -> log.error("[THRESHOLD] 超时定时器失败 nodeId={}: {}", nodeId, e.getMessage()));
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
        saveSpecialNodeState(ctx, nodeId, NodeStatus.TIMEOUT, timeoutOutput);
        writeTraceEnd(ctx, node, NodeResult.timeout(targetNodeId,
                "SPECIAL_NODE_TIMEOUT", label + " 等待超时"), 0);

        if (targetNodeId == null || targetNodeId.isBlank()) {
            ctxStore.delete(ctx.getCanvasId(), ctx.getUserId());
            reactiveTaskRegistry.submit(
                    "dag-timeout-complete-" + ctx.getExecutionId() + "-" + nodeId,
                    executionService.completePausedExecution(ctx, ExecutionStatus.FAILED.getCode(), timeoutOutput),
                    e -> log.error("[{}] 超时终态落库失败 nodeId={}: {}", label, nodeId, e.getMessage()));
            return;
        }

        ctxStore.save(ctx);
        reactiveTaskRegistry.submit(
                "dag-timeout-continuation-" + ctx.getExecutionId() + "-" + nodeId,
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
                        }),
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
        if (!nodeGateCoordinator.tryAcquireOrSignalRepeat(nodeGate)) {
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
                        if (ctx.isBenefitGranted() || ctx.isUserReached()) return Mono.just(Map.of());
                        return triggerFailureAwareDownstream(graph, nodeId, node.getType(), ctx, depth,
                                result.errorMessage());
                    }
                    // ── 路径B：WAITING（THRESHOLD 阈值未满足）─────────────────
                    // executeHandlerWithRepeat 对 success=true 的结果会走 repeat 检查，
                    // 检查完后已执行 nodeGate.executing.set(false) 释放锁，此处不需再操作。
                    if (result.pending()) {
                        if (shouldCommitOutput(result)) {
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
                    if (shouldCommitOutput(result)) {
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
                    nodeGateCoordinator.release(nodeGate);
                    ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                    if (ctx.isBenefitGranted() || ctx.isUserReached()) {
                        return Mono.just(Map.of());
                    }
                    return Mono.error(e);
                });
    }

    private Mono<Map<String, Object>> triggerDownstream(DagGraph graph, NodeResult result,
                                                        String sourceNodeId, String sourceType,
                                                        ExecutionContext ctx,
                                                        int depth) {
        // 立即标记未走的分支入口为 SKIPPED（设计文档 7.7节两阶段写入 - 阶段一）
        // 目的：让汇聚节点在执行中即时检测到上游 SKIPPED，而不是等到执行结束
        nodeResultRouter.markNonTakenBranchesSkipped(graph, sourceNodeId, sourceType, result, ctx);

        // 收集所有下游并行触发
        List<String> nextIds = nodeResultRouter.nextNodeIds(result);
        if (nextIds.isEmpty()) {
            // 叶子节点直接返回自身输出，作为本次 DAG 执行响应的一部分。
            return Mono.just(result.output() != null ? result.output() : Map.of());
        }
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
        List<String> nextIds = nodeResultRouter.failureAwareDownstream(graph, sourceNodeId);

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
            DagParser.CanvasNode currentNode = graph.getNode(current);
            clearSpecialTimeoutGeneration(ctx, current, currentNode == null ? null : currentNode.getType());
            if (ctx.getExecutionId() != null && !ctx.getExecutionId().isBlank()) {
                try {
                    ctxStore.deleteNodeState(ctx.getExecutionId(), current);
                } catch (RuntimeException e) {
                    log.warn("[ENGINE] reset 删除节点增量状态失败 executionId={} nodeId={}: {}",
                            ctx.getExecutionId(), current, e.getMessage());
                }
            }
            if (current.equals(sourceNodeId)) {
                continue;
            }
            graph.downstream(current).forEach(queue::addLast);
        }
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
                        .tenantId(ctx.getTenantId())
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
                .tenantId(ctx.getTenantId())
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
                .tenantId(ctx.getTenantId())
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

    // ══════════════════════════════════════════════════════════════
    // 工具方法
    // ══════════════════════════════════════════════════════════════

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

    private boolean shouldCommitOutput(NodeResult result) {
        if (result == null || result.output() == null || result.output().isEmpty()) {
            return false;
        }
        if (!result.success() || result.pending()) {
            return false;
        }
        return result.outcome() == null || result.outcome() == NodeOutcome.SUCCESS;
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
    }

}
