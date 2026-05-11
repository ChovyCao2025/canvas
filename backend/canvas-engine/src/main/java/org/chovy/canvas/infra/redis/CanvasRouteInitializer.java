package org.chovy.canvas.infra.redis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasVersion;
import org.chovy.canvas.domain.canvas.CanvasVersionMapper;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 服务重启后触发路由表全量重建（设计文档第 6.4 节）。
 *
 * Redis 路由表是内存数据，服务或 Redis 重启后可能丢失。
 * 启动时检查路由表是否完整，若为空则从 MySQL 全量重建。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasRouteInitializer {

    private final CanvasMapper          canvasMapper;
    private final CanvasVersionMapper   canvasVersionMapper;
    private final DagParser             dagParser;
    private final TriggerRouteService   triggerRouteService;

    @PostConstruct
    public void initTriggerRoutes() {
        if (!triggerRouteService.isRouteTableEmpty()) {
            log.info("[ROUTE_INIT] 路由表已存在，跳过重建");
            return;
        }

        log.warn("[ROUTE_INIT] 触发路由表为空，从 MySQL 全量重建...");

        List<Canvas> published = canvasMapper.selectList(
                new LambdaQueryWrapper<Canvas>().eq(Canvas::getStatus, 1));

        int count = 0;
        for (Canvas canvas : published) {
            if (canvas.getPublishedVersionId() == null) continue;
            try {
                CanvasVersion version = canvasVersionMapper.selectById(canvas.getPublishedVersionId());
                if (version == null) continue;

                DagGraph graph = dagParser.parse(version.getGraphJson());
                registerRoutes(canvas.getId(), graph);
                count++;
            } catch (Exception e) {
                log.error("[ROUTE_INIT] 重建失败 canvasId={}: {}", canvas.getId(), e.getMessage());
            }
        }

        log.info("[ROUTE_INIT] 路由表重建完成，共处理 {} 个已发布画布", count);
    }

    @SuppressWarnings("unchecked")
    private void registerRoutes(Long canvasId, DagGraph graph) {
        for (String nodeId : graph.entryNodes()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || node.getConfig() == null) continue;
            Map<String, Object> cfg = node.getConfig();
            switch (node.getType()) {
                case "MQ_TRIGGER"      -> { String k = (String) cfg.get("topicKey");   if (k != null) triggerRouteService.registerMq(canvasId, k); }
                case "BEHAVIOR_IN_APP" -> { String k = (String) cfg.get("eventCode");  if (k != null) triggerRouteService.registerBehavior(canvasId, k); }
                case "TAGGER_REALTIME" -> { String k = (String) cfg.get("tagCodeKey"); if (k != null) triggerRouteService.registerTagger(canvasId, k); }
                default -> {}
            }
        }
    }
}
