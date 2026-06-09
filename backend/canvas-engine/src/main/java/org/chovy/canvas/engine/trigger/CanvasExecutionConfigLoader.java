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

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回 loadCanvasForDryRun 流程生成的业务结果。
     */
    CanvasDO loadCanvasForDryRun(Long canvasId) {
        CanvasDO canvas = canvasMapper.selectById(canvasId);
        if (canvas == null) {
            throw new IllegalStateException("画布不存在: " + canvasId);
        }
        return canvas;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param dryRun dry run 参数，用于 validateAndLoadCanvas 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param graphJson JSON 字符串，承载结构化配置或明细。
     * @return 返回解析、归一化或安全处理后的值。
     */
    DagGraph parseGraph(String graphJson) {
        return dagParser.parse(graphJson);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param versionId 业务对象 ID，用于定位具体记录。
     * @return 返回 loadGraph 流程生成的业务结果。
     */
    DagGraph loadGraph(Long canvasId, Long versionId) {
        return configCache.get(canvasId, versionId);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     */
    void invalidateCanvas(Long canvasId) {
        canvasEntityCache.invalidate(canvasId);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param canvas canvas 参数，用于 resolveVersionId 流程中的校验、计算或对象转换。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param dryRun dry run 参数，用于 resolveVersionId 流程中的校验、计算或对象转换。
     * @return 返回 resolve version id 计算得到的数量、金额或指标值。
     */
    Long resolveVersionId(CanvasDO canvas, String userId, boolean dryRun) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (dryRun) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param graph graph 参数，用于 findTriggerNode 流程中的校验、计算或对象转换。
     * @param triggerNodeType 类型标识，用于选择对应处理分支。
     * @param matchKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    String findTriggerNode(DagGraph graph, String triggerNodeType, String matchKey) {
        log.debug("[FIND_TRIGGER] triggerNodeType={} matchKey={}", triggerNodeType, matchKey);

        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param triggerNodeType 类型标识，用于选择对应处理分支。
     * @return 返回布尔判断结果。
     */
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
