package com.photon.canvas.engine.handlers;

import com.photon.canvas.domain.canvas.Canvas;
import com.photon.canvas.domain.canvas.CanvasMapper;
import com.photon.canvas.domain.canvas.CanvasVersion;
import com.photon.canvas.domain.canvas.CanvasVersionMapper;
import com.photon.canvas.engine.context.ExecutionContext;
import com.photon.canvas.engine.dag.DagGraph;
import com.photon.canvas.engine.handler.NodeHandler;
import com.photon.canvas.engine.handler.NodeHandlerType;
import com.photon.canvas.engine.handler.NodeResult;
import com.photon.canvas.engine.scheduler.DagEngine;
import com.photon.canvas.infra.cache.CanvasConfigCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 子流程引用节点（设计文档第二十章）。
 *
 * subFlowVersion = -1 → 动态取最新已发布版本
 * subFlowVersion = N  → 锁定到指定版本
 *
 * inputMapping：父上下文字段 → 子流程输入
 * outputPrefix：子流程输出字段加此前缀写入父上下文，防冲突
 *
 * 子流程类型：
 *   - WORKFLOW：完整 DAG 执行（与 CANVAS_TRIGGER 类似，不注册外部触发）
 *   - STRATEGY_TABLE / DATA_TABLE：按 graph_json 中内嵌数据直接查找（不走 DAG 引擎）
 */
@Slf4j
@NodeHandlerType("SUB_FLOW_REF")
@RequiredArgsConstructor
public class SubFlowRefHandler implements NodeHandler {

    private final CanvasMapper        canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;
    private final CanvasConfigCache   configCache;
    private final DagEngine           dagEngine;

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        Object subFlowIdObj = config.get("subFlowId");
        if (subFlowIdObj == null) return NodeResult.fail("SUB_FLOW_REF 缺少 subFlowId");
        Long   subFlowId      = Long.parseLong(String.valueOf(subFlowIdObj));
        int    subFlowVersion = config.get("subFlowVersion") instanceof Number n ? n.intValue() : -1;
        String outputPrefix   = (String) config.getOrDefault("outputPrefix", "sf");
        String nextNodeId     = (String) config.get("nextNodeId");
        Map<String, Object> inputMapping =
                (Map<String, Object>) config.getOrDefault("inputMapping", Map.of());

        // 防循环
        if (ctx.getCallStack().contains(subFlowId)) {
            return NodeResult.fail("SUB_FLOW_REF 循环调用: " + subFlowId);
        }

        // 解析版本：-1 表示最新发布版
        Canvas canvas = canvasMapper.selectById(subFlowId);
        if (canvas == null || canvas.getStatus() != 1) {
            return NodeResult.fail("子流程画布未发布: " + subFlowId);
        }
        Long versionId = subFlowVersion == -1
                ? canvas.getPublishedVersionId()
                : resolveVersion(subFlowId, subFlowVersion);

        if (versionId == null) {
            return NodeResult.fail("子流程版本不存在: " + subFlowVersion);
        }

        // 构建子流程执行上下文
        ExecutionContext childCtx = new ExecutionContext();
        childCtx.setExecutionId(ctx.getExecutionId() + ":subflow:" + UUID.randomUUID().toString().substring(0, 6));
        childCtx.setCanvasId(subFlowId);
        childCtx.setVersionId(versionId);
        childCtx.setUserId(ctx.getUserId());
        childCtx.setTriggerType("SUB_FLOW_REF");
        childCtx.getCallStack().addAll(ctx.getCallStack());
        childCtx.getCallStack().add(ctx.getCanvasId());

        // inputMapping：把父上下文字段映射到子流程
        inputMapping.forEach((childKey, parentKeyExpr) -> {
            String parentKey = String.valueOf(parentKeyExpr).replace("ctx.", "");
            Object val = ctx.getContextValue(parentKey);
            if (val != null) childCtx.getTriggerPayload().put(childKey, val);
        });

        try {
            DagGraph graph     = configCache.get(subFlowId, versionId);
            String triggerNode = graph.entryNodes().isEmpty() ? null : graph.entryNodes().get(0);
            if (triggerNode == null) return NodeResult.fail("子流程无触发器节点");

            // 执行子流程（同步等待，Timeout 级联：从父流程剩余预算中扣减，此处简化为300s）
            Map<String, Object> childOutput = dagEngine.execute(graph, triggerNode, childCtx)
                    .block(Duration.ofSeconds(300));

            // 将子流程输出带前缀写入父上下文
            Map<String, Object> prefixedOutput = new HashMap<>();
            if (childOutput != null) {
                childOutput.forEach((k, v) -> prefixedOutput.put(outputPrefix + "_" + k, v));
            }
            // 也合并子流程 flatContext（包含所有节点输出）
            childCtx.getFlatContext().forEach((k, v) ->
                    prefixedOutput.putIfAbsent(outputPrefix + "_" + k, v));

            log.info("[SUB_FLOW_REF] 子流程完成 subFlowId={} outputKeys={}",
                    subFlowId, prefixedOutput.keySet());
            return NodeResult.ok(nextNodeId, prefixedOutput);
        } catch (Exception e) {
            log.error("[SUB_FLOW_REF] 执行失败: {}", e.getMessage());
            return NodeResult.fail("子流程执行失败: " + e.getMessage());
        }
    }

    private Long resolveVersion(Long canvasId, int version) {
        return canvasVersionMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CanvasVersion>()
                                .eq(CanvasVersion::getCanvasId, canvasId)
                                .eq(CanvasVersion::getVersion, version))
                .stream().findFirst().map(CanvasVersion::getId).orElse(null);
    }
}
