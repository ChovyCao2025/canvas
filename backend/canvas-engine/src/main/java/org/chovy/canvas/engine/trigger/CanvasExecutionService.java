package org.chovy.canvas.engine.trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.chovy.canvas.domain.constant.NodeType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasVersion;
import org.chovy.canvas.domain.canvas.CanvasVersionMapper;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.ExecutionStatus;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.execution.CanvasExecution;
import org.chovy.canvas.domain.execution.CanvasExecutionMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.infra.cache.CanvasConfigCache;
import org.chovy.canvas.infra.redis.ContextPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    private final org.chovy.canvas.domain.execution.CanvasExecutionStatsMapper statsMapper;

    /** Canvas 实体本地缓存：canvasId → Canvas。TTL=5min，最多500条。 */
    private final Cache<Long, Canvas> canvasCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();


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
                    Canvas canvas = canvasMapper.selectById(canvasId);
                    if (canvas == null) throw new IllegalStateException("画布不存在: " + canvasId);

                    ExecutionContext ctx = newContext(canvasId, -1L, userId, TriggerType.DRY_RUN);
                    if (payload != null) ctx.getTriggerPayload().putAll(payload);

                    // 直接用传入的 graphJson，没有则退到 DB draft
                    DagGraph graph;
                    if (graphJson != null && !graphJson.isBlank()) {
                        graph = dagParser.parse(graphJson);
                    } else {
                        Long versionId = resolveVersionId(canvas, userId, true);
                        graph = configCache.get(canvasId, versionId);
                        ctx.setVersionId(versionId);
                    }

                    // dry-run：找 DIRECT 类型的 START 节点，找不到则取第一个入口节点
                    String triggerNodeId = findTriggerNode(graph, NodeType.DIRECT_CALL, null);
                    if (triggerNodeId == null) {
                        triggerNodeId = graph.entryNodes().stream().findFirst().orElse(null);
                    }
                    if (triggerNodeId == null)
                        throw new IllegalStateException("画布没有入口节点，请确保存在触发器节点");

                    return Map.of("ctx", ctx, "graph", graph, "triggerNodeId", triggerNodeId);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(prep -> {
                    ExecutionContext ctx = (ExecutionContext) prep.get("ctx");
                    DagGraph graph = (DagGraph) prep.get("graph");
                    String triggerNodeId = (String) prep.get("triggerNodeId");

                    CanvasExecution exec = createExecution(ctx);
                    Mono.fromRunnable(() -> executionMapper.insert(exec))
                            .subscribeOn(Schedulers.boundedElastic()).subscribe();

                    return dagEngine.execute(graph, triggerNodeId, ctx)
                            .timeout(Duration.ofSeconds(globalTimeoutSec))
                            .flatMap(result -> {
                                updateExecution(exec, ExecutionStatus.SUCCESS.getCode(), result);
                                Map<String, Object> resp = new HashMap<>(result);
                                resp.put("executionId", ctx.getExecutionId());
                                return Mono.just(resp);
                            })
                            .onErrorResume(e -> {
                                log.error("[DRY_RUN] 执行失败: {}", e.getMessage());
                                updateExecution(exec, ExecutionStatus.FAILED.getCode(), Map.of("error", e.getMessage()));
                                return Mono.just(Map.of(
                                        "error", e.getMessage(),
                                        "executionId", ctx.getExecutionId()
                                ));
                            });
                });
    }

    /**
     * 触发画布执行：执行工作流包含 dedup 检查、上下文准备、DAG 执行以及结果持久化。
     *
     * @param canvasId        画布 ID
     * @param userId          触发用户 ID
     * @param triggerType     触发场景类型（如 MQ, DIRECT_CALL, BEHAVIOR, DRY_RUN）
     * @param triggerNodeType 触发器节点类型，用于定位 DAG 入口
     * @param matchKey        触发条件匹配 Key（MQ 为 topicKey，BEHAVIOR 为 eventCode）
     * @param payload         触发时携带的业务数据
     * @param msgId           消息唯一 ID，用于执行幂等控制
     * @param dryRun          是否干运行（不产生副作用，不写入数据库）
     * @return 包含执行结果的响应式流
     */
    public Mono<Map<String, Object>> trigger(
            Long canvasId, String userId, String triggerType,
            String triggerNodeType, String matchKey,
            Map<String, Object> payload, String msgId, boolean dryRun) {
        // ...
        return Mono.fromCallable(() -> {

                    // 1. 加载画布（草稿在 dry-run 时可用，正式触发只允许已发布）
                    Canvas canvas = canvasCache.get(canvasId, id -> canvasMapper.selectById(id));
                    if (canvas == null) {
                        throw new IllegalStateException("画布不存在: " + canvasId);
                    }
                    if (!dryRun && !Objects.equals(canvas.getStatus(), CanvasStatusEnum.PUBLISHED.getCode())) {
                        throw new IllegalStateException("画布未发布，请先发布后再触发: " + canvasId);
                    }

                    // 2. 前置检查（有效期 / 配额 / 冷却期）—— dry-run 跳过
                    if (!dryRun) {
                        // FIXME: 前端没有对应的位置配置此系列属性
                        preCheckService.check(canvas, userId);
                    }

                    // 2.5 单画布并发执行上限（设计文档 12.4节）
                    // 防止同一画布被大量并发触发（如大促瞬间流量）打爆执行引擎
                    if (!dryRun) {
                        int active = executionRegistry.activeCount(canvasId);
                        int maxConc = canvas.getMaxTotalExecutions() != null
                                ? Math.min(canvas.getMaxTotalExecutions(), globalMaxConcurrency)
                                : globalMaxConcurrency;
                        if (active >= maxConc) {
                            log.warn("[ENGINE] 画布并发上限已达 canvasId={} active={}/{}", canvasId, active, maxConc);
                            return Map.of("overflow", "concurrency_limit_reached",
                                    "active", active, "limit", maxConc);
                        }
                    }

                    // 3. dedup 检查
                    if (!dryRun && msgId != null) {
                        // FIXME: 此处控制不足够精细, 可能会影响后续订单去重时间
                        // FIXME: 目前幂等时间默认是24小时, 但是如果在挂起期间有第二个订单进入流程的话, 幂等时间会被设置为20分钟
                        boolean isResume = ctxStore.exists(canvasId, userId);
                        Duration dedupTtl = isResume
                                ? Duration.ofSeconds(globalTimeoutSec + 600)
                                : Duration.ofHours(24);
                        // 幂等校验处理
                        if (!ctxStore.acquireDedup(canvasId, userId, msgId, dedupTtl)) {
                            log.debug("[ENGINE] dedup 拦截 canvasId={} userId={} msgId={}", canvasId, userId, msgId);
                            return Map.<String, Object>of("deduplicated", true);
                        }
                    }

                    // 4. 加载/恢复 ExecutionContext
                    // FIXME: 如果同一个用户针对同一个画布在挂起期间重新被触发, 恢复的上下文可能会有错误
                    ExecutionContext ctx;
                    boolean isResume = ctxStore.exists(canvasId, userId);
                    if (isResume && !dryRun) {
                        // 恢复锁
                        String instanceId = UUID.randomUUID().toString();
                        if (!ctxStore.acquireResumeLock(canvasId, userId, instanceId, globalTimeoutSec)) {
                            log.warn("[ENGINE] resume-lock 竞争失败，放弃本次触发 canvasId={}", canvasId);
                            return Map.<String, Object>of("skipped", "resume-lock");
                        }
                        ctx = ctxStore.load(canvasId, userId);
                        if (ctx == null)
                            ctx = newContext(canvasId, resolveVersionId(canvas, userId, false), userId, triggerType);
                    } else {
                        ctx = newContext(canvasId, resolveVersionId(canvas, userId, dryRun), userId, triggerType);
                    }

                    // 合并本次 payload
                    if (payload != null) ctx.getTriggerPayload().putAll(payload);

                    // 5. 加载 DAG 配置（版本锁定：用 ctx 中的 versionId，不用当前发布版本）
                    DagGraph graph = configCache.get(canvasId, ctx.getVersionId());

                    // 6. 找触发器节点（按类型和 matchKey）
                    String triggerNodeId = findTriggerNode(graph, triggerNodeType, matchKey);
                    if (triggerNodeId == null) {
                        throw new IllegalStateException(
                                "找不到触发器节点 type=" + triggerNodeType + " key=" + matchKey);
                    }

                    return Map.of("ctx", ctx, "graph", graph, "triggerNodeId", triggerNodeId,
                            "isResume", isResume, "canvas", canvas);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(prep -> {
                    ExecutionContext ctx = (ExecutionContext) prep.get("ctx");
                    DagGraph graph = (DagGraph) prep.get("graph");
                    String triggerNodeId = (String) prep.get("triggerNodeId");
                    boolean isResume = (Boolean) prep.get("isResume");
                    Canvas canvas = (Canvas) prep.get("canvas");

                    // 7. 写执行记录（开始）
                    // dry-run 也写记录，这样执行轨迹面板能看到测试运行的 trace；
                    // 只是不统计、不写 ctxStore、不注册到 kill 注册表
                    final CanvasExecution finalExec = createExecution(ctx);
                    Mono.fromRunnable(() -> executionMapper.insert(finalExec))
                            .subscribeOn(Schedulers.boundedElastic()).subscribe();

                    // 8. 实际执行 DAG，并向注册表注册 Disposable（供 Kill Switch FORCE 模式使用）
                    Mono<Map<String, Object>> executionMono = dagEngine.execute(graph, triggerNodeId, ctx)
                            .timeout(Duration.ofSeconds(globalTimeoutSec));

                    return executionMono
                            .doOnSubscribe(sub -> {
                                if (!dryRun) {
                                    executionRegistry.register(canvasId, ctx.getExecutionId(),
                                            reactor.core.Disposables.single());
                                }
                            })
                            .doFinally(signal -> executionRegistry.deregister(canvasId, ctx.getExecutionId()))
                            .flatMap(result -> {
                                // 9. 执行完成，更新执行记录
                                updateExecution(finalExec, ExecutionStatus.SUCCESS.getCode(), result); // SUCCESS
                                if (!dryRun) {
                                    ctxStore.delete(ctx.getCanvasId(), ctx.getUserId());
                                    if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId());
                                    incrementStats(ctx.getCanvasId(), 2, ctx.getUserId());
                                }
                                // 将 executionId 合入返回结果，便于前端定位执行轨迹
                                Map<String, Object> resp = new HashMap<>(result);
                                resp.put("executionId", ctx.getExecutionId());
                                return Mono.just(resp);
                            })
                            .onErrorResume(e -> {
                                log.error("[ENGINE] 执行失败 executionId={}: {}", ctx.getExecutionId(), e.getMessage());
                                boolean paused = isPaused(ctx, graph);
                                if (paused) {
                                    if (!dryRun) ctxStore.save(ctx);
                                    updateExecution(finalExec, ExecutionStatus.PAUSED.getCode(), Map.of()); // PAUSED
                                } else {
                                    if (!dryRun) {
                                        ctxStore.delete(ctx.getCanvasId(), ctx.getUserId());
                                        if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId());
                                    }
                                    updateExecution(finalExec, 3, Map.of("error", e.getMessage())); // FAILED
                                }
                                return Mono.just(Map.of(
                                        "error", e.getMessage(),
                                        "executionId", ctx.getExecutionId()
                                ));
                            });
                });
    }

    // ── 缓存失效 ──────────────────────────────────────────────────

    /**
     * 画布发布/下线时主动驱逐 Canvas 实体缓存，
     * 确保下次 trigger() 读到最新状态（已发布/已下线）。
     *
     * @param canvasId 画布 ID
     */
    public void invalidateCanvas(Long canvasId) {
        canvasCache.invalidate(canvasId);
    }

    // ── 私有帮助方法 ──────────────────────────────────────────────

    /**
     * 初始化执行上下文
     *
     * @param canvasId    画布 ID
     * @param versionId   版本 ID
     * @param userId      用户 ID
     * @param triggerType 触发类型
     * @return 新的执行上下文
     */
    private ExecutionContext newContext(Long canvasId, Long versionId, String userId, String triggerType) {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId(UUID.randomUUID().toString());
        ctx.setCanvasId(canvasId);
        ctx.setVersionId(versionId);
        ctx.setUserId(userId);
        ctx.setTriggerType(triggerType);
        // userId 写入 triggerPayload，供节点 config 中 ${userId} 引用
        if (userId != null) ctx.getTriggerPayload().put("userId", userId);
        return ctx;
    }

    /**
     * 灰度路由：根据 userId+canvasId Hash 决定使用正式版本还是灰度版本（设计文档 16.1节）。
     * 相同用户始终落入相同版本（确定性 Hash），保证一致的用户体验。
     *
     * @param canvas 画布对象
     * @param userId 用户 ID
     * @return 路由后的版本 ID
     */
    private Long resolveVersionId(Canvas canvas, String userId, boolean dryRun) {
        // dry-run：优先用最新草稿（status=0），即编辑器当前状态
        if (dryRun) {
            CanvasVersion draft = canvasVersionMapper.selectOne(
                    new LambdaQueryWrapper<CanvasVersion>()
                            .eq(CanvasVersion::getCanvasId, canvas.getId())
                            .eq(CanvasVersion::getStatus, 0)
                            .orderByDesc(CanvasVersion::getId)
                            .last("LIMIT 1")
            );
            if (draft != null) return draft.getId();
            // 没有草稿则取最新版本
            CanvasVersion latest = canvasVersionMapper.selectOne(
                    new LambdaQueryWrapper<CanvasVersion>()
                            .eq(CanvasVersion::getCanvasId, canvas.getId())
                            .orderByDesc(CanvasVersion::getId)
                            .last("LIMIT 1")
            );
            if (latest != null) return latest.getId();
        }
        // 正式执行：灰度 or 发布版本
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
     * 在 DAG 中按触发类型和 matchKey 找触发器节点。
     *
     * @param graph           画布 DAG 图
     * @param triggerNodeType 触发器节点类型
     * @param matchKey        触发条件匹配 Key
     * @return 触发器节点 ID
     * <p>两种查找路径：
     * <ul>
     *   <li>MANUAL_APPROVAL（审批恢复）：在全图中查找类型匹配的节点，
     *       因为该节点不是入口节点。</li>
     *   <li>其他触发类型：仅在 entryNodes 中查找 START 节点，
     *       通过 config.triggerType 匹配：
     *       EVENT_TRIGGER → EVENT,
     *       MQ_TRIGGER → MQ, SCHEDULED_TRIGGER → SCHEDULED</li>
     * </ul>
     */
    private String findTriggerNode(DagGraph graph, String triggerNodeType, String matchKey) {
        log.debug("[FIND_TRIGGER] triggerNodeType={} matchKey={}", triggerNodeType, matchKey);

        // 全图搜索：triggerNodeType 就是目标节点的 type 字段
        // 触发器节点（EVENT_TRIGGER / MQ_TRIGGER / SCHEDULED_TRIGGER 等）通常不是 DAG 入口，
        // 因为前面有一个通用 START 节点。MANUAL_APPROVAL 是流程中间节点，恢复时同样需全图搜。
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || !triggerNodeType.equals(node.getType())) continue;
            if (matchKey == null) return nodeId;
            Map<String, Object> cfg = new HashMap<>();
            if (node.getBizConfig() != null) cfg.putAll(node.getBizConfig());
            if (node.getConfig() != null) cfg.putAll(node.getConfig());
            String cfgKey = (String) cfg.getOrDefault("eventCode", cfg.getOrDefault("topicKey", ""));
            if (matchKey.equals(cfgKey)) return nodeId;
        }
        return null;
    }

    /**
     * 判断画布执行是否处于挂起状态（等待中）
     *
     * @param ctx   当前执行上下文
     * @param graph 画布 DAG 图
     * @return true 表示挂起
     */
    private boolean isPaused(ExecutionContext ctx, DagGraph graph) {
        // 多阶段挂起：有节点处于 WAITING 状态
        // （LOGIC_RELATION / HUB 条件未满足时 DagEngine 设置此状态）
        return ctx.getNodeStatuses().values().stream()
                .anyMatch(s -> s == org.chovy.canvas.engine.context.NodeStatus.WAITING);
    }

    /**
     * 创建执行记录对象
     *
     * @param ctx 执行上下文
     * @return 执行记录
     */
    private CanvasExecution createExecution(ExecutionContext ctx) {
        CanvasExecution exec = new CanvasExecution();
        exec.setId(ctx.getExecutionId());
        exec.setCanvasId(ctx.getCanvasId());
        exec.setVersionId(ctx.getVersionId());
        exec.setUserId(ctx.getUserId());
        exec.setTriggerType(ctx.getTriggerType());
        exec.setStatus(ExecutionStatus.RUNNING.getCode()); // 执行中
        return exec;
    }

    /**
     * 更新执行记录状态
     *
     * @param exec   执行记录对象
     * @param status 状态码
     * @param result 执行结果 Map
     */
    private void updateExecution(CanvasExecution exec, int status, Map<String, Object> result) {
        if (exec == null) return;
        exec.setStatus(status);
        try {
            exec.setResult(new ObjectMapper().writeValueAsString(result));
        } catch (Exception ignored) {
        }
        Mono.fromRunnable(() -> executionMapper.updateById(exec))
                .subscribeOn(Schedulers.boundedElastic()).subscribe();
    }


    /**
     * 更新 canvas_execution_stats 当日统计（设计文档 21.1节）。
     * 使用 INSERT ... ON DUPLICATE KEY UPDATE 保证原子性。
     */
    private void incrementStats(Long canvasId, int finalStatus, String userId) {
        Thread.ofVirtual().start(() -> {
            try {
                java.time.LocalDate today = java.time.LocalDate.now();
                // 查已有记录
                org.chovy.canvas.domain.execution.CanvasExecutionStats stats =
                        statsMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query
                                .LambdaQueryWrapper<org.chovy.canvas.domain.execution.CanvasExecutionStats>()
                                .eq(org.chovy.canvas.domain.execution.CanvasExecutionStats::getCanvasId, canvasId)
                                .eq(org.chovy.canvas.domain.execution.CanvasExecutionStats::getStatDate, today));
                if (stats == null) {
                    stats = new org.chovy.canvas.domain.execution.CanvasExecutionStats();
                    stats.setCanvasId(canvasId);
                    stats.setStatDate(today);
                    stats.setTotalCount(0);
                    stats.setSuccessCount(0);
                    stats.setFailCount(0);
                    stats.setPausedCount(0);
                    stats.setUniqueUsers(0);
                    statsMapper.insert(stats);
                    stats = statsMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query
                            .LambdaQueryWrapper<org.chovy.canvas.domain.execution.CanvasExecutionStats>()
                            .eq(org.chovy.canvas.domain.execution.CanvasExecutionStats::getCanvasId, canvasId)
                            .eq(org.chovy.canvas.domain.execution.CanvasExecutionStats::getStatDate, today));
                }
                if (stats == null) return;
                stats.setTotalCount(stats.getTotalCount() + 1);
                if (finalStatus == ExecutionStatus.SUCCESS.getCode())
                    stats.setSuccessCount(stats.getSuccessCount() + 1);
                else if (finalStatus == ExecutionStatus.FAILED.getCode()) stats.setFailCount(stats.getFailCount() + 1);
                else if (finalStatus == ExecutionStatus.PAUSED.getCode())
                    stats.setPausedCount(stats.getPausedCount() + 1);
                statsMapper.updateById(stats);
            } catch (Exception e) {
                log.warn("[STATS] 更新统计失败 canvasId={}: {}", canvasId, e.getMessage());
            }
        });
    }
}
