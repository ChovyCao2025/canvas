package org.chovy.canvas.infra.redis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasVersion;
import org.chovy.canvas.domain.canvas.CanvasVersionMapper;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqRouteRefreshService {

    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;
    private final DagParser dagParser;
    private final TriggerRouteService triggerRouteService;
    private final MqTriggerHandler mqTriggerHandler;

    public void rebuildMqRoutes() {
        List<Canvas> published = canvasMapper.selectList(
                new LambdaQueryWrapper<Canvas>().eq(Canvas::getStatus, CanvasStatusEnum.PUBLISHED.getCode()));
        List<Long> versionIds = published.stream()
                .map(Canvas::getPublishedVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, CanvasVersion> versionMap = versionIds.isEmpty()
                ? Map.of()
                : canvasVersionMapper.selectBatchIds(versionIds).stream()
                .collect(Collectors.toMap(CanvasVersion::getId, v -> v));

        int canvasCount = 0;
        int routeCount = 0;
        Map<String, Set<String>> routes = new HashMap<>();
        for (Canvas canvas : published) {
            CanvasVersion version = versionMap.get(canvas.getPublishedVersionId());
            if (version == null || version.getGraphJson() == null) {
                continue;
            }
            try {
                DagGraph graph = dagParser.parse(version.getGraphJson());
                routeCount += collectMqRoutes(canvas.getId(), graph, routes);
                canvasCount++;
            } catch (Exception e) {
                log.error("[MQ_ROUTE] rebuild failed canvasId={}: {}", canvas.getId(), e.getMessage(), e);
            }
        }
        triggerRouteService.replaceMqRoutes(routes);
        log.info("[MQ_ROUTE] rebuild completed canvases={} routes={}", canvasCount, routeCount);
    }

    private int collectMqRoutes(Long canvasId, DagGraph graph, Map<String, Set<String>> routes) {
        int count = 0;
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || !NodeType.MQ_TRIGGER.equals(node.getType())) {
                continue;
            }
            Map<String, Object> config = new HashMap<>();
            if (node.getBizConfig() != null) {
                config.putAll(node.getBizConfig());
            }
            if (node.getConfig() != null) {
                config.putAll(node.getConfig());
            }
            String topic = mqTriggerHandler.resolveTopic(config);
            if (topic != null && !topic.isBlank()) {
                routes.computeIfAbsent(topic, ignored -> new HashSet<>()).add(String.valueOf(canvasId));
                count++;
            }
        }
        return count;
    }
}
