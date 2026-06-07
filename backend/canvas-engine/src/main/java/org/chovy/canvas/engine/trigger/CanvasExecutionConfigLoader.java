package org.chovy.canvas.engine.trigger;

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
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.cache.CanvasEntityCache;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Loads canvas execution configuration and resolves DAG entry nodes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasExecutionConfigLoader {

    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;
    private final CanvasConfigCache configCache;
    private final CanvasEntityCache canvasEntityCache;
    private final DagParser dagParser;
    private final MqTriggerHandler mqTriggerHandler;

    CanvasDO loadCanvasForDryRun(Long canvasId) {
        CanvasDO canvas = canvasMapper.selectById(canvasId);
        if (canvas == null) {
            throw new IllegalStateException("画布不存在: " + canvasId);
        }
        return canvas;
    }

    CanvasDO validateAndLoadCanvas(Long canvasId, boolean dryRun) {
        CanvasDO canvas = canvasEntityCache.get(canvasId);
        if (canvas == null) {
            throw new IllegalStateException("画布不存在: " + canvasId);
        }
        if (!dryRun && !Objects.equals(canvas.getStatus(), CanvasStatusEnum.PUBLISHED.getCode())) {
            throw new IllegalStateException("画布未发布，请先发布后再触发: " + canvasId);
        }
        return canvas;
    }

    DagGraph parseGraph(String graphJson) {
        return dagParser.parse(graphJson);
    }

    DagGraph loadGraph(Long canvasId, Long versionId) {
        return configCache.get(canvasId, versionId);
    }

    void invalidateCanvas(Long canvasId) {
        canvasEntityCache.invalidate(canvasId);
    }

    Long resolveVersionId(CanvasDO canvas, String userId, boolean dryRun) {
        if (dryRun) {
            CanvasVersionDO draft = canvasVersionMapper.selectOne(
                    new LambdaQueryWrapper<CanvasVersionDO>()
                            .eq(CanvasVersionDO::getCanvasId, canvas.getId())
                            .eq(CanvasVersionDO::getStatus, 0)
                            .orderByDesc(CanvasVersionDO::getId)
                            .last("LIMIT 1")
            );
            if (draft != null) return draft.getId();
            CanvasVersionDO latest = canvasVersionMapper.selectOne(
                    new LambdaQueryWrapper<CanvasVersionDO>()
                            .eq(CanvasVersionDO::getCanvasId, canvas.getId())
                            .orderByDesc(CanvasVersionDO::getId)
                            .last("LIMIT 1")
            );
            if (latest != null) return latest.getId();
        }
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

    String findTriggerNode(DagGraph graph, String triggerNodeType, String matchKey) {
        log.debug("[FIND_TRIGGER] triggerNodeType={} matchKey={}", triggerNodeType, matchKey);

        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || !triggerNodeType.equals(node.getType())) continue;
            if (matchesByNodeId(triggerNodeType)) {
                if (matchKey == null || nodeId.equals(matchKey)) return nodeId;
                continue;
            }
            if (matchKey == null) return nodeId;
            Map<String, Object> cfg = new HashMap<>();
            if (node.getBizConfig() != null) cfg.putAll(node.getBizConfig());
            if (node.getConfig() != null) cfg.putAll(node.getConfig());
            String cfgKey = switch (triggerNodeType) {
                case NodeType.MQ_TRIGGER -> mqTriggerHandler.resolveTopic(cfg);
                default -> (String) cfg.getOrDefault("eventCode", cfg.getOrDefault("topicKey", ""));
            };
            if (matchKey.equals(cfgKey)) return nodeId;
        }
        return null;
    }

    private boolean matchesByNodeId(String triggerNodeType) {
        return NodeType.WAIT.equals(triggerNodeType)
                || NodeType.HUB.equals(triggerNodeType)
                || NodeType.USER_INPUT.equals(triggerNodeType)
                || NodeType.MANUAL_APPROVAL.equals(triggerNodeType)
                || NodeType.AGGREGATE.equals(triggerNodeType)
                || NodeType.THRESHOLD.equals(triggerNodeType)
                || NodeType.SCHEDULED_TRIGGER.equals(triggerNodeType)
                || NodeType.TAGGER.equals(triggerNodeType);
    }
}
