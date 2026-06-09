package org.chovy.canvas.infrastructure.redis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rebuilds runtime Redis routes and local schedule registrations from published canvas versions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerRouteRecoveryService {

    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;
    private final DagParser dagParser;
    private final TriggerRouteService triggerRouteService;
    private final MqTriggerHandler mqTriggerHandler;
    private final CanvasSchedulerService schedulerService;

    /** Rebuild Redis trigger routes and local schedule registrations from the DB source of truth. */
    public RecoveryReport rebuildRuntimeState() {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        Map<Long, DagGraph> publishedGraphs = loadPublishedGraphs();
        TriggerRouteService.TriggerRouteSnapshot routeSnapshot = buildRouteSnapshot(publishedGraphs);
        triggerRouteService.replaceAllTriggerRoutes(routeSnapshot);
        int scheduledRegistrations = schedulerService.replaceScheduledTriggers(publishedGraphs);
        RecoveryReport report = new RecoveryReport(
                publishedGraphs.size(),
                routeMembershipCount(routeSnapshot.mqRoutes()),
                routeMembershipCount(routeSnapshot.behaviorRoutes()),
                routeMembershipCount(routeSnapshot.taggerRoutes()),
                scheduledRegistrations
        );
        log.info("[ROUTE_RECOVERY] runtime state rebuilt {}", report);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return report;
    }

    /** Load published canvas graphs keyed by canvas id. Dirty records are skipped and logged. */
    Map<Long, DagGraph> loadPublishedGraphs() {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<CanvasDO> published = canvasMapper.selectList(
                new LambdaQueryWrapper<CanvasDO>().eq(CanvasDO::getStatus, CanvasStatusEnum.PUBLISHED.getCode()));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<Long> versionIds = published.stream()
                .map(CanvasDO::getPublishedVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, CanvasVersionDO> versionMap = versionIds.isEmpty()
                ? Map.of()
                : canvasVersionMapper.selectBatchIds(versionIds).stream()
                .collect(Collectors.toMap(CanvasVersionDO::getId, version -> version));

        Map<Long, DagGraph> graphs = new LinkedHashMap<>();
        for (CanvasDO canvas : published) {
            CanvasVersionDO version = versionMap.get(canvas.getPublishedVersionId());
            if (version == null || version.getGraphJson() == null) {
                continue;
            }
            try {
                graphs.put(canvas.getId(), dagParser.parse(version.getGraphJson()));
            } catch (Exception e) {
                log.error("[ROUTE_RECOVERY] parse failed canvasId={}: {}", canvas.getId(), e.getMessage(), e);
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return graphs;
    }

    /** Build a full Redis route snapshot from parsed published graphs. */
    TriggerRouteService.TriggerRouteSnapshot buildRouteSnapshot(Map<Long, DagGraph> publishedGraphs) {
        Map<String, Set<String>> mqRoutes = new HashMap<>();
        Map<String, Set<String>> behaviorRoutes = new HashMap<>();
        Map<String, Set<String>> taggerRoutes = new HashMap<>();
        publishedGraphs.forEach((canvasId, graph) -> collectRoutes(canvasId, graph, mqRoutes, behaviorRoutes, taggerRoutes));
        return new TriggerRouteService.TriggerRouteSnapshot(mqRoutes, behaviorRoutes, taggerRoutes);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param graph graph 参数，用于 collectRoutes 流程中的校验、计算或对象转换。
     * @param mqRoutes mq routes 参数，用于 collectRoutes 流程中的校验、计算或对象转换。
     * @param behaviorRoutes behavior routes 参数，用于 collectRoutes 流程中的校验、计算或对象转换。
     * @param taggerRoutes tagger routes 参数，用于 collectRoutes 流程中的校验、计算或对象转换。
     */
    private void collectRoutes(Long canvasId,
                               DagGraph graph,
                               Map<String, Set<String>> mqRoutes,
                               Map<String, Set<String>> behaviorRoutes,
                               Map<String, Set<String>> taggerRoutes) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (node == null) {
                continue;
            }
            Map<String, Object> config = mergedConfig(node);
            switch (node.getType()) {
                case NodeType.EVENT_TRIGGER -> {
                    String eventCode = stringValue(config.get("eventCode"));
                    addRoute(behaviorRoutes, eventCode, canvasId);
                }
                case NodeType.MQ_TRIGGER -> addRoute(mqRoutes, mqTriggerHandler.resolveTopic(config), canvasId);
                case NodeType.TAGGER -> {
                    if ("realtime".equals(String.valueOf(config.getOrDefault("mode", "")))) {
                        addRoute(taggerRoutes, stringValue(config.get("tagCodeKey")), canvasId);
                    }
                }
                default -> {
                }
            }
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param node node 参数，用于 mergedConfig 流程中的校验、计算或对象转换。
     * @return 返回 mergedConfig 流程生成的业务结果。
     */
    private Map<String, Object> mergedConfig(DagParser.CanvasNode node) {
        Map<String, Object> config = new HashMap<>();
        if (node.getBizConfig() != null) {
            config.putAll(node.getBizConfig());
        }
        if (node.getConfig() != null) {
            config.putAll(node.getConfig());
        }
        return config;
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param MapString map string 参数，用于 addRoute 流程中的校验、计算或对象转换。
     * @param routes routes 参数，用于 addRoute 流程中的校验、计算或对象转换。
     * @param routeKey 业务键，用于在同一租户下定位资源。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     */
    private void addRoute(Map<String, Set<String>> routes, String routeKey, Long canvasId) {
        if (routeKey == null || routeKey.isBlank() || canvasId == null) {
            return;
        }
        routes.computeIfAbsent(routeKey, ignored -> new java.util.HashSet<>())
                .add(String.valueOf(canvasId));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string value 生成的文本或业务键。
     */
    private String stringValue(Object value) {
        return value instanceof String text ? text : null;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 routeMembershipCount 流程中的校验、计算或对象转换。
     * @param routes routes 参数，用于 routeMembershipCount 流程中的校验、计算或对象转换。
     * @return 返回 route membership count 计算得到的数量、金额或指标值。
     */
    private int routeMembershipCount(Map<String, Set<String>> routes) {
        return routes.values().stream().mapToInt(Set::size).sum();
    }

    /** Summary returned by the manual recovery command. */
    public record RecoveryReport(
            int publishedCanvasCount,
            int mqRouteCount,
            int behaviorRouteCount,
            int taggerRouteCount,
            int scheduledRegistrationCount
    ) {
    }
}
