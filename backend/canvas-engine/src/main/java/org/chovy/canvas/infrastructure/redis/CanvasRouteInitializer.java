package org.chovy.canvas.infrastructure.redis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

/**
 * 服务重启后触发路由表全量重建（设计文档第 6.4 节）。
 *
 * 多实例并发启动时加分布式锁，防止重复重建：
 *   - SETNX canvas:route-init:lock → 只有一个实例执行重建
 *   - TTL 120s，重建完成后主动释放
 *   - 其他实例等待 2s 后检查是否已重建，若已有路由则跳过
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasRouteInitializer {

    /** 画布数据访问组件，用于扫描已发布画布。 */
    private final CanvasMapper          canvasMapper;
    /** 画布版本数据访问组件，用于批量读取发布版本。 */
    private final CanvasVersionMapper   canvasVersionMapper;
    /** DAG 解析器，用于从 graphJson 恢复触发节点配置。 */
    private final DagParser             dagParser;
    /** 触发路由服务，用于注册启动重建得到的路由。 */
    private final TriggerRouteService   triggerRouteService;
    /** MQ 触发处理器，用于复用 topic 解析语义。 */
    private final MqTriggerHandler      mqTriggerHandler;
    /** Redis 模板，用于启动重建锁和路由状态标记。 */
    private final StringRedisTemplate   redis;

    /** 启动路由重建的分布式锁 key。 */
    private static final String REBUILD_LOCK = "canvas:route-init:lock";

    /**
     * 启动阶段路由初始化。
     * 仅在“路由表为空”时重建，避免每次重启都全量扫描发布画布。
     */
    @PostConstruct
    public void initTriggerRoutes() {
        if (!triggerRouteService.isRouteTableEmpty()) {
            // Redis 中已有任意触发路由时只补 ready 标记，避免重启时重复扫描 MySQL 和改写路由表。
            triggerRouteService.markRouteReady();
            log.info("[ROUTE_INIT] 路由表已存在，跳过重建");
            return;
        }

        // 分布式锁：多实例并发启动时只有一个实例执行重建
        String lockValue = UUID.randomUUID().toString();
        boolean acquired = Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(REBUILD_LOCK, lockValue, Duration.ofSeconds(120)));

        if (!acquired) {
            // 另一实例正在重建，等待 2s 后不再重建（它会完成）
            log.info("[ROUTE_INIT] 另一实例正在重建路由表，本实例跳过");
            // 等待窗口给持锁实例完成 ready 标记；本实例不抢重建，减少启动风暴下的重复写。
            LockSupport.parkNanos(Duration.ofSeconds(2).toNanos());
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                log.warn("[ROUTE_INIT] 等待其他实例重建路由时被中断");
            }
            return;
        }

        try {
            // 重建期间先标记不可用，MQ 消费端会抛异常交给 RocketMQ 重试而不是丢消息。
            triggerRouteService.markRouteRebuilding();
            log.warn("[ROUTE_INIT] 触发路由表为空，从 MySQL 全量重建...");
            List<CanvasDO> published = canvasMapper.selectList(
                    new LambdaQueryWrapper<CanvasDO>().eq(CanvasDO::getStatus, CanvasStatusEnum.PUBLISHED.getCode()));

            // 批量查询版本（避免 N+1）
            List<Long> versionIds = published.stream()
                    .map(CanvasDO::getPublishedVersionId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            Map<Long, CanvasVersionDO> versionMap = versionIds.isEmpty()
                    ? Map.of()
                    : canvasVersionMapper.selectBatchIds(versionIds).stream()
                            .collect(java.util.stream.Collectors.toMap(CanvasVersionDO::getId, v -> v));

            int count = 0;
            for (CanvasDO canvas : published) {
                if (canvas.getPublishedVersionId() == null) continue;
                try {
                    CanvasVersionDO version = versionMap.get(canvas.getPublishedVersionId());
                    if (version == null || version.getGraphJson() == null) continue;
                    DagGraph graph = dagParser.parse(version.getGraphJson());
                    registerRoutes(canvas.getId(), graph);
                    count++;
                } catch (Exception e) {
                    log.error("[ROUTE_INIT] 重建失败 canvasId={}: {}", canvas.getId(), e.getMessage());
                }
            }
            // 所有可解析画布处理完后再发布 ready 标记，消费端从这里开始读取新路由。
            triggerRouteService.markRouteReady();
            log.info("[ROUTE_INIT] 路由表重建完成，共处理 {} 个已发布画布", count);
        } finally {
            // 释放锁
            redis.delete(REBUILD_LOCK);
        }
    }

    /**
     * 注册、调度或初始化 register Routes 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param graph graph 方法执行所需的业务参数
     */
    @SuppressWarnings("unchecked")
    private void registerRoutes(Long canvasId, DagGraph graph) {
        // 与 CanvasService.publish 的注册逻辑保持同构，保证“启动重建”和“发布增量”行为一致
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null) continue;
            Map<String, Object> cfg = new java.util.HashMap<>();
            if (node.getBizConfig() != null) cfg.putAll(node.getBizConfig());
            if (node.getConfig()    != null) cfg.putAll(node.getConfig());
            switch (node.getType()) {
                // EVENT_TRIGGER：eventCode 精确路由
                case NodeType.EVENT_TRIGGER -> { String k = (String) cfg.get("eventCode"); if (k != null) triggerRouteService.registerBehavior(canvasId, k); }
                // MQ_TRIGGER：topicKey 可能来自不同字段，复用 handler 的解析逻辑
                case NodeType.MQ_TRIGGER       -> { String k = mqTriggerHandler.resolveTopic(cfg); if (!k.isEmpty()) triggerRouteService.registerMq(canvasId, k); }
                case NodeType.TAGGER -> { if ("realtime".equals(String.valueOf(cfg.getOrDefault("mode", "")))) { String k = (String) cfg.get("tagCodeKey"); if (k != null) triggerRouteService.registerTagger(canvasId, k); } }
                default -> {}
            }
        }
    }
}
