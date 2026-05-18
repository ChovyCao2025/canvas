package org.chovy.canvas.infra.redis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasVersion;
import org.chovy.canvas.domain.canvas.CanvasVersionMapper;
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

    private final CanvasMapper          canvasMapper;
    private final CanvasVersionMapper   canvasVersionMapper;
    private final DagParser             dagParser;
    private final TriggerRouteService   triggerRouteService;
    private final MqTriggerHandler      mqTriggerHandler;
    private final StringRedisTemplate   redis;

    private static final String REBUILD_LOCK = "canvas:route-init:lock";

    @PostConstruct
    public void initTriggerRoutes() {
        if (!triggerRouteService.isRouteTableEmpty()) {
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
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            return;
        }

        try {
            log.warn("[ROUTE_INIT] 触发路由表为空，从 MySQL 全量重建...");
            List<Canvas> published = canvasMapper.selectList(
                    new LambdaQueryWrapper<Canvas>().eq(Canvas::getStatus, CanvasStatusEnum.PUBLISHED.getCode()));

            // 批量查询版本（避免 N+1）
            List<Long> versionIds = published.stream()
                    .map(Canvas::getPublishedVersionId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            Map<Long, CanvasVersion> versionMap = versionIds.isEmpty()
                    ? Map.of()
                    : canvasVersionMapper.selectBatchIds(versionIds).stream()
                            .collect(java.util.stream.Collectors.toMap(CanvasVersion::getId, v -> v));

            int count = 0;
            for (Canvas canvas : published) {
                if (canvas.getPublishedVersionId() == null) continue;
                try {
                    CanvasVersion version = versionMap.get(canvas.getPublishedVersionId());
                    if (version == null || version.getGraphJson() == null) continue;
                    DagGraph graph = dagParser.parse(version.getGraphJson());
                    registerRoutes(canvas.getId(), graph);
                    count++;
                } catch (Exception e) {
                    log.error("[ROUTE_INIT] 重建失败 canvasId={}: {}", canvas.getId(), e.getMessage());
                }
            }
            log.info("[ROUTE_INIT] 路由表重建完成，共处理 {} 个已发布画布", count);
        } finally {
            // 释放锁
            redis.delete(REBUILD_LOCK);
        }
    }

    @SuppressWarnings("unchecked")
    private void registerRoutes(Long canvasId, DagGraph graph) {
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null) continue;
            Map<String, Object> cfg = new java.util.HashMap<>();
            if (node.getBizConfig() != null) cfg.putAll(node.getBizConfig());
            if (node.getConfig()    != null) cfg.putAll(node.getConfig());
            switch (node.getType()) {
                case NodeType.EVENT_TRIGGER -> { String k = (String) cfg.get("eventCode"); if (k != null) triggerRouteService.registerBehavior(canvasId, k); }
                case NodeType.MQ_TRIGGER       -> { String k = mqTriggerHandler.resolveTopic(cfg); if (!k.isEmpty()) triggerRouteService.registerMq(canvasId, k); }
                case NodeType.TAGGER_REALTIME  -> { String k = (String) cfg.get("tagCodeKey"); if (k != null) triggerRouteService.registerTagger(canvasId, k); }
                default -> {}
            }
        }
    }
}
