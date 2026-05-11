package com.photon.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;

/**
 * 子流程引用节点（设计文档第二十章）。
 *
 * 三种子流程类型（由子流程画布 graph_json 顶层 type 字段标识）：
 *   WORKFLOW       — 完整 DAG 执行，走 DagEngine
 *   STRATEGY_TABLE — 多因子精确匹配（按 order 顺序，* 为通配）
 *   DATA_TABLE     — 按 lookupKey 查列值
 *
 * subFlowVersion = -1 → 动态取最新已发布版本
 * outputPrefix：子流程输出字段加此前缀写入父上下文，防冲突
 */
@Slf4j
@NodeHandlerType("SUB_FLOW_REF")
@RequiredArgsConstructor
public class SubFlowRefHandler implements NodeHandler {

    private final CanvasMapper        canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;
    private final CanvasConfigCache   configCache;
    private final DagEngine           dagEngine;
    private final ObjectMapper        objectMapper;

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

        Canvas canvas = canvasMapper.selectById(subFlowId);
        if (canvas == null || canvas.getStatus() != 1) {
            return NodeResult.fail("子流程画布未发布: " + subFlowId);
        }
        Long versionId = subFlowVersion == -1
                ? canvas.getPublishedVersionId()
                : resolveVersion(subFlowId, subFlowVersion);
        if (versionId == null) return NodeResult.fail("子流程版本不存在: " + subFlowVersion);

        // 加载子流程 graph_json
        CanvasVersion version = canvasVersionMapper.selectById(versionId);
        if (version == null) return NodeResult.fail("子流程版本记录不存在");

        // 解析 graph_json 顶层结构，判断子流程类型
        Map<String, Object> graphRoot;
        try {
            graphRoot = objectMapper.readValue(version.getGraphJson(), Map.class);
        } catch (Exception e) {
            return NodeResult.fail("子流程 JSON 解析失败: " + e.getMessage());
        }

        String subFlowType = (String) graphRoot.getOrDefault("type", "WORKFLOW");

        // 构建输入数据（父上下文字段 → 子流程）
        Map<String, Object> inputData = new HashMap<>();
        inputMapping.forEach((childKey, parentKeyExpr) -> {
            String parentKey = String.valueOf(parentKeyExpr).replace("ctx.", "");
            Object val = ctx.getContextValue(parentKey);
            if (val != null) inputData.put(childKey, val);
        });

        Map<String, Object> subOutput;
        try {
            subOutput = switch (subFlowType) {
                case "STRATEGY_TABLE" -> executeStrategyTable(graphRoot, inputData, ctx);
                case "DATA_TABLE"     -> executeDataTable(graphRoot, inputData, ctx);
                default               -> executeWorkflow(subFlowId, versionId, inputData, ctx);
            };
        } catch (Exception e) {
            log.error("[SUB_FLOW_REF] 执行失败 type={}: {}", subFlowType, e.getMessage());
            return NodeResult.fail("子流程执行失败: " + e.getMessage());
        }

        if (subOutput == null) {
            // 无匹配（STRATEGY_TABLE/DATA_TABLE 未找到）→ 失败
            return NodeResult.fail("子流程无匹配结果");
        }

        // 带前缀写入父上下文
        Map<String, Object> prefixed = new HashMap<>();
        subOutput.forEach((k, v) -> prefixed.put(outputPrefix + "_" + k, v));

        log.info("[SUB_FLOW_REF] 完成 type={} subFlowId={} outputKeys={}",
                subFlowType, subFlowId, prefixed.keySet());
        return NodeResult.ok(nextNodeId, prefixed);
    }

    // ══ STRATEGY_TABLE（设计文档 20.3节）════════════════════════

    /**
     * 多因子精确匹配算法（设计文档 20.5节）：
     * 按 order 升序遍历策略，"*" 为通配（匹配任意值），
     * 未指定因子 = 通配，精确条件优先于模糊条件由运营在 order 上体现。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> executeStrategyTable(Map<String, Object> graphRoot,
                                                       Map<String, Object> inputData,
                                                       ExecutionContext ctx) {
        List<Map<String, Object>> strategies =
                (List<Map<String, Object>>) graphRoot.getOrDefault("strategies", List.of());

        strategies.sort(Comparator.comparingInt(s ->
                s.get("order") instanceof Number n ? n.intValue() : 0));

        for (Map<String, Object> strategy : strategies) {
            Map<String, Object> conditions =
                    (Map<String, Object>) strategy.getOrDefault("conditions", Map.of());

            boolean matched = conditions.entrySet().stream().allMatch(e -> {
                String expected = String.valueOf(e.getValue());
                if ("*".equals(expected)) return true; // 通配
                // 从 inputData（子流程输入）或 ctx（父上下文）取实际值
                Object actual = inputData.containsKey(e.getKey())
                        ? inputData.get(e.getKey())
                        : ctx.getContextValue(e.getKey());
                return actual != null && expected.equals(String.valueOf(actual));
            });

            if (matched) {
                Map<String, Object> result =
                        (Map<String, Object>) strategy.getOrDefault("result", Map.of());
                log.debug("[STRATEGY_TABLE] 命中策略 id={} name={}",
                        strategy.get("id"), strategy.get("name"));
                return new HashMap<>(result);
            }
        }

        return null; // 无匹配
    }

    // ══ DATA_TABLE（设计文档 20.4节）════════════════════════════

    /**
     * 按 lookupKey 查列值（精确匹配 column.key）。
     * lookupKey 来自 config.lookupKey 或 ctx 取值。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> executeDataTable(Map<String, Object> graphRoot,
                                                   Map<String, Object> inputData,
                                                   ExecutionContext ctx) {
        String lookupKeyField = (String) graphRoot.get("lookupKey");
        String lookupValue;
        if (lookupKeyField != null) {
            Object v = inputData.containsKey(lookupKeyField)
                    ? inputData.get(lookupKeyField)
                    : ctx.getContextValue(lookupKeyField);
            lookupValue = v != null ? String.valueOf(v) : null;
        } else {
            // 取 inputData 第一个值
            lookupValue = inputData.values().stream()
                    .findFirst().map(String::valueOf).orElse(null);
        }

        if (lookupValue == null) return null;

        List<Map<String, Object>> columns =
                (List<Map<String, Object>>) graphRoot.getOrDefault("columns", List.of());

        return columns.stream()
                .filter(c -> lookupValue.equals(String.valueOf(c.get("key"))))
                .findFirst()
                .map(c -> new HashMap<>((Map<String, Object>) c.getOrDefault("values", Map.of())))
                .orElse(null); // 未找到 → null → 父流程失败
    }

    // ══ WORKFLOW（设计文档 20.5节）══════════════════════════════

    private Map<String, Object> executeWorkflow(Long subFlowId, Long versionId,
                                                  Map<String, Object> inputData,
                                                  ExecutionContext ctx) {
        ExecutionContext childCtx = new ExecutionContext();
        childCtx.setExecutionId(ctx.getExecutionId() + ":sf:" +
                UUID.randomUUID().toString().substring(0, 6));
        childCtx.setCanvasId(subFlowId);
        childCtx.setVersionId(versionId);
        childCtx.setUserId(ctx.getUserId());
        childCtx.setTriggerType("SUB_FLOW_REF");
        childCtx.getCallStack().addAll(ctx.getCallStack());
        childCtx.getCallStack().add(ctx.getCanvasId());
        childCtx.getTriggerPayload().putAll(inputData);

        DagGraph graph = configCache.get(subFlowId, versionId);
        if (graph.entryNodes().isEmpty()) return null;

        Map<String, Object> output = dagEngine.execute(graph, graph.entryNodes().get(0), childCtx)
                .block(Duration.ofSeconds(300));

        // 合并子流程 flatContext（所有节点输出）
        Map<String, Object> merged = new HashMap<>(childCtx.getFlatContext());
        if (output != null) merged.putAll(output);
        return merged;
    }

    // ── helper ───────────────────────────────────────────────────

    private Long resolveVersion(Long canvasId, int version) {
        return canvasVersionMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CanvasVersion>()
                                .eq(CanvasVersion::getCanvasId, canvasId)
                                .eq(CanvasVersion::getVersion, version))
                .stream().findFirst().map(CanvasVersion::getId).orElse(null);
    }
}
