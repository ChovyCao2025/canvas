package org.chovy.canvas.infrastructure.redis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.config.CanvasRuntimeMetrics;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mq Route Refresh Redis 基础设施组件。
 *
 * <p>封装触发路由、分布式状态或缓存 key 的 Redis 访问逻辑，减少业务层直接拼接 key。
 * <p>该组件是运行时路由和高并发治理的重要边界，需要保持 key 语义稳定。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqRouteRefreshService {

    /** 画布 Mapper，用于扫描所有已发布画布。 */
    private final CanvasMapper canvasMapper;
    /** 画布版本 Mapper。 */
    private final CanvasVersionMapper canvasVersionMapper;
    /** DAG 解析器，将 graphJson 转换为可执行图结构。 */
    private final DagParser dagParser;
    /** 触发路由服务，负责 Redis 路由注册、清理和查询。 */
    private final TriggerRouteService triggerRouteService;
    /** MQ 触发节点处理器，用于解析和匹配 MQ 触发入口。 */
    private final MqTriggerHandler mqTriggerHandler;
    /** 运行时运维指标。 */
    private CanvasRuntimeMetrics runtimeMetrics;

    @Autowired(required = false)
    void setRuntimeMetrics(CanvasRuntimeMetrics runtimeMetrics) {
        this.runtimeMetrics = runtimeMetrics;
    }

    /** 扫描已发布画布并重建全部触发路由表。 */
    public void rebuildTriggerRoutes() {
        // 路由重建只以“已发布画布 + 已发布版本”为数据源，草稿版本不进入运行时触发路由。
        List<CanvasDO> published = canvasMapper.selectList(
                new LambdaQueryWrapper<CanvasDO>().eq(CanvasDO::getStatus, CanvasStatusEnum.PUBLISHED.getCode()));
        List<Long> versionIds = published.stream()
                .map(CanvasDO::getPublishedVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, CanvasVersionDO> versionMap = versionIds.isEmpty()
                ? Map.of()
                : canvasVersionMapper.selectBatchIds(versionIds).stream()
                .collect(Collectors.toMap(CanvasVersionDO::getId, v -> v));

        int canvasCount = 0;
        int routeCount = 0;
        Map<String, Set<String>> mqRoutes = new HashMap<>();
        Map<String, Set<String>> behaviorRoutes = new HashMap<>();
        Map<String, Set<String>> taggerRoutes = new HashMap<>();
        for (CanvasDO canvas : published) {
            CanvasVersionDO version = versionMap.get(canvas.getPublishedVersionId());
            if (version == null || version.getGraphJson() == null) {
                // 发布记录缺少可解析版本时跳过，避免单个脏数据阻断整批路由重建。
                continue;
            }
            try {
                DagGraph graph = dagParser.parse(version.getGraphJson());
                routeCount += collectTriggerRoutes(canvas.getId(), graph, mqRoutes, behaviorRoutes, taggerRoutes);
                canvasCount++;
            } catch (Exception e) {
                log.error("[TRIGGER_ROUTE] rebuild failed canvasId={}: {}", canvas.getId(), e.getMessage(), e);
                recordRouteRebuildFailure("canvas_parse");
            }
        }
        try {
            triggerRouteService.replaceAllTriggerRoutes(
                    new TriggerRouteService.TriggerRouteSnapshot(mqRoutes, behaviorRoutes, taggerRoutes));
        } catch (RuntimeException e) {
            recordRouteRebuildFailure(e.getClass().getSimpleName());
            throw e;
        }
        log.info("[TRIGGER_ROUTE] rebuild completed canvases={} routes={}", canvasCount, routeCount);
    }

    /** 扫描已发布画布并重建触发路由表。保留旧入口以兼容 MQ 配置变更调用方。 */
    public void rebuildMqRoutes() {
        rebuildTriggerRoutes();
    }

    /** 从单个画布 DAG 中收集三类触发路由到画布 ID 的关系。 */
    private int collectTriggerRoutes(Long canvasId,
                                     DagGraph graph,
                                     Map<String, Set<String>> mqRoutes,
                                     Map<String, Set<String>> behaviorRoutes,
                                     Map<String, Set<String>> taggerRoutes) {
        int count = 0;
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null) {
                continue;
            }
            Map<String, Object> config = new HashMap<>();
            if (node.getBizConfig() != null) {
                config.putAll(node.getBizConfig());
            }
            if (node.getConfig() != null) {
                config.putAll(node.getConfig());
            }
            switch (node.getType()) {
                case NodeType.MQ_TRIGGER -> {
                    String topic = mqTriggerHandler.resolveTopic(config);
                    if (topic != null && !topic.isBlank()) {
                        mqRoutes.computeIfAbsent(topic, ignored -> new HashSet<>()).add(String.valueOf(canvasId));
                        count++;
                    }
                }
                case NodeType.EVENT_TRIGGER -> {
                    String eventCode = (String) config.get("eventCode");
                    if (eventCode != null && !eventCode.isBlank()) {
                        behaviorRoutes.computeIfAbsent(eventCode, ignored -> new HashSet<>())
                                .add(String.valueOf(canvasId));
                        count++;
                    }
                }
                case NodeType.TAGGER -> {
                    if (!"realtime".equals(String.valueOf(config.getOrDefault("mode", "")))) {
                        continue;
                    }
                    String tagCodeKey = (String) config.get("tagCodeKey");
                    if (tagCodeKey != null && !tagCodeKey.isBlank()) {
                        taggerRoutes.computeIfAbsent(tagCodeKey, ignored -> new HashSet<>())
                                .add(String.valueOf(canvasId));
                        count++;
                    }
                }
                default -> {
                }
            }
        }
        return count;
    }

    private void recordRouteRebuildFailure(String reason) {
        if (runtimeMetrics != null) {
            runtimeMetrics.recordRouteRebuildFailure(reason);
        }
    }
}
