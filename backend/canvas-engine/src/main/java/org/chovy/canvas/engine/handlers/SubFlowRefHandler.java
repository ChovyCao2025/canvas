package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.domain.canvas.SubFlowLookupService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

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
@Component
@Slf4j
@NodeHandlerType("SUB_FLOW_REF")
public class SubFlowRefHandler implements NodeHandler {

    /** 子流程查询服务，用于隔离 handler 与持久层细节。 */
    private final SubFlowLookupService subFlowLookupService;

    /** 画布配置缓存，用于加载 WORKFLOW 子流程 DAG。 */
    private final CanvasConfigCache   configCache;

    /** DAG 执行引擎，用于运行 WORKFLOW 类型子流程。 */
    private final DagEngine           dagEngine;

    /** JSON 序列化器，用于解析子流程 graph_json。 */
    private final ObjectMapper        objectMapper;

    /**
     * 构造子流程引用节点处理器。
     *
     * @param subFlowLookupService 子流程画布与版本查询服务
     * @param configCache 画布 DAG 配置缓存，用于加载 WORKFLOW 子流程
     * @param dagEngine DAG 执行引擎，延迟注入以避免节点处理器循环依赖
     * @param objectMapper JSON 解析器，用于读取子流程 graph_json
     */
    public SubFlowRefHandler(SubFlowLookupService subFlowLookupService,
                             CanvasConfigCache configCache,
                             @Lazy DagEngine dagEngine,
                             ObjectMapper objectMapper) {
        this.subFlowLookupService = subFlowLookupService;
        this.configCache          = configCache;
        this.dagEngine            = dagEngine;
        this.objectMapper         = objectMapper;
    }

    /**
     * 执行子流程引用节点：解析目标子流程版本、映射父上下文输入，并按子流程类型选择执行路径。
     *
     * <p>STRATEGY_TABLE 和 DATA_TABLE 在当前节点内完成匹配并把结果写入父流程输出；WORKFLOW 会创建隔离的
     * 子执行上下文并调用 {@link DagEngine} 运行目标 DAG，完成后给子流程输出加 outputPrefix 前缀再回写父流程。
     * 本方法不直接开启事务；画布读取走查询服务和配置缓存，WORKFLOW 执行可能触发子流程内节点的外部副作用。
     * 调用栈包含目标子流程时会直接失败，避免递归调用。</p>
     *
     * @param config 节点配置，关键字段包括 subFlowId、subFlowVersion、inputMapping、outputPrefix 和 nextNodeId
     * @param ctx 父流程执行上下文，提供用户、执行 ID、上下文变量与调用栈
     * @return 节点结果；表类子流程返回匹配输出，WORKFLOW 返回加前缀后的子流程输出
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        Object subFlowIdObj = config.get("subFlowId");
        if (subFlowIdObj == null) return Mono.just(NodeResult.fail("SUB_FLOW_REF 缺少 subFlowId"));
        Long   subFlowId      = Long.parseLong(String.valueOf(subFlowIdObj));
        int    subFlowVersion = config.get("subFlowVersion") instanceof Number n ? n.intValue() : -1;
        String outputPrefix   = (String) config.getOrDefault("outputPrefix", "sf");
        String nextNodeId     = (String) config.get(MapFieldKeys.NEXT_NODE_ID);
        Map<String, Object> inputMapping =
                (Map<String, Object>) config.getOrDefault("inputMapping", Map.of());

        // 防循环
        if (ctx.getCallStack().contains(subFlowId)) {
            // 调用栈中已存在目标子流程，说明会形成递归调用。
            return Mono.just(NodeResult.fail("SUB_FLOW_REF 循环调用: " + subFlowId));
        }

        return Mono.fromCallable(() -> prepareSubFlow(
                        subFlowId, subFlowVersion, outputPrefix, nextNodeId, inputMapping, config, ctx))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(prepared -> {
                    if (prepared.failure() != null) {
                        return Mono.just(prepared.failure());
                    }
                    return switch (prepared.subFlowType()) {
                        case "STRATEGY_TABLE" -> {
                            // 策略表子流程在当前线程内做多因子匹配，不启动完整 DAG。
                            Map<String, Object> r = executeStrategyTable(prepared.graphRoot(), prepared.inputData(), ctx);
                            yield r != null ? Mono.just(NodeResult.ok(prepared.nextNodeId(), r))
                                            : Mono.just(NodeResult.fail("STRATEGY_TABLE 无匹配策略"));
                        }
                        case "DATA_TABLE" -> {
                            // 数据表子流程按 lookupKey 查列值，未命中视为业务失败。
                            Map<String, Object> r = executeDataTable(prepared.graphRoot(), prepared.inputData(), ctx);
                            yield r != null ? Mono.just(NodeResult.ok(prepared.nextNodeId(), r))
                                            : Mono.just(NodeResult.fail("DATA_TABLE 未找到 key"));
                        }
                        default -> executeWorkflow(prepared.subFlowId(), prepared.versionId(), prepared.inputData(),
                                ctx, prepared.nextNodeId(), prepared.outputPrefix())
                                .onErrorResume(e -> Mono.just(NodeResult.fail("子流程执行失败: " + e.getMessage())));
                    };
                })
                .onErrorResume(e -> Mono.just(NodeResult.fail("SUB_FLOW_REF: " + e.getMessage())));
    }

    /**
     * 加载并校验子流程画布、版本和 graph_json，同时按 inputMapping 从父上下文抽取子流程输入。
     */
    private PreparedSubFlow prepareSubFlow(Long subFlowId,
                                           int subFlowVersion,
                                           String outputPrefix,
                                           String nextNodeId,
                                           Map<String, Object> inputMapping,
                                           Map<String, Object> config,
                                           ExecutionContext ctx) {
        CanvasDO canvas = subFlowLookupService.findCanvas(subFlowId);
        if (canvas == null || canvas.getStatus() != 1) {
            return PreparedSubFlow.failure(NodeResult.fail("子流程画布未发布: " + subFlowId));
        }
        Long versionId = subFlowVersion == -1
                ? canvas.getPublishedVersionId()
                : subFlowLookupService.resolveVersionId(subFlowId, subFlowVersion);
        // subFlowVersion=-1 使用当前发布版本；指定版本则按版本号解析。
        if (versionId == null) {
            return PreparedSubFlow.failure(NodeResult.fail("子流程版本不存在: " + subFlowVersion));
        }

        // 加载子流程 graph_json
        CanvasVersionDO version = subFlowLookupService.findVersion(versionId);
        if (version == null) {
            return PreparedSubFlow.failure(NodeResult.fail("子流程版本记录不存在"));
        }

        // 解析 graph_json 顶层结构，判断子流程类型
        Map<String, Object> graphRoot;
        try {
            graphRoot = objectMapper.readValue(version.getGraphJson(), Map.class);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return PreparedSubFlow.failure(NodeResult.fail("子流程 JSON 解析失败: " + e.getMessage()));
        }

        String subFlowType = (String) graphRoot.getOrDefault("type", "WORKFLOW");

        // 构建输入数据（父上下文字段 → 子流程）
        Map<String, Object> inputData = new HashMap<>();
        inputMapping.forEach((childKey, parentKeyExpr) -> {
            String parentKey = String.valueOf(parentKeyExpr).replace("ctx.", "");
            Object val = ctx.getContextValue(parentKey);
            if (val != null) inputData.put(childKey, val);
        });

        return new PreparedSubFlow(null, subFlowType, graphRoot, inputData, subFlowId,
                versionId, nextNodeId, outputPrefix);
    }

    /**
     * 子流程执行前准备结果。
     *
     * @param failure 前置失败结果
     * @param subFlowType 子流程类型
     * @param graphRoot 子流程图 JSON 根对象
     * @param inputData 传给子流程的输入数据
     * @param subFlowId 子流程画布 ID
     * @param versionId 子流程版本 ID
     * @param nextNodeId 子流程完成后的下游节点 ID
     * @param outputPrefix 输出前缀
     */
    private record PreparedSubFlow(
            NodeResult failure,
            String subFlowType,
            Map<String, Object> graphRoot,
            Map<String, Object> inputData,
            Long subFlowId,
            Long versionId,
            String nextNodeId,
            String outputPrefix
    ) {
        /**
         * 构造前置失败的子流程准备结果。
         *
         * @param failure 失败节点结果
         * @return 失败准备结果
         */
        static PreparedSubFlow failure(NodeResult failure) {
            return new PreparedSubFlow(failure, null, null, null, null, null, null, null);
        }
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

    /**
     * 执行 WORKFLOW 子流程，使用独立子上下文运行目标 DAG，并把子上下文输出加前缀后返回父流程。
     */
    private Mono<NodeResult> executeWorkflow(Long subFlowId, Long versionId,
                                              Map<String, Object> inputData,
                                              ExecutionContext ctx,
                                              String nextNodeId,
                                              String outputPrefix) {
        ExecutionContext childCtx = new ExecutionContext();
        childCtx.setExecutionId(ctx.getExecutionId() + ":sf:" + UUID.randomUUID().toString().substring(0, 6));
        childCtx.setCanvasId(subFlowId);
        childCtx.setVersionId(versionId);
        childCtx.setUserId(ctx.getUserId());
        childCtx.setTriggerType(TriggerType.SUB_FLOW_REF);
        childCtx.getCallStack().addAll(ctx.getCallStack());
        childCtx.getCallStack().add(ctx.getCanvasId());
        childCtx.putTriggerPayloadValues(inputData);

        // WORKFLOW 子流程复用 DagEngine 执行，使用独立 childCtx 隔离节点状态。
        DagGraph graph = configCache.get(subFlowId, versionId);
        if (graph.entryNodes().isEmpty()) return Mono.just(NodeResult.fail("子流程无触发器节点"));

        return dagEngine.execute(graph, graph.entryNodes().get(0), childCtx)
                .map(output -> {
                    // 合并子流程所有输出，加 outputPrefix 前缀写回父上下文防 key 冲突
                    Map<String, Object> merged = new HashMap<>(childCtx.exportContextValues());
                    if (output != null) merged.putAll(output);
                    Map<String, Object> prefixed = new HashMap<>();
                    merged.forEach((k, v) -> prefixed.put(outputPrefix + "_" + k, v));
                    log.info("[SUB_FLOW_REF] WORKFLOW 完成 subFlowId={} prefix={} outputKeys={}",
                            subFlowId, outputPrefix, prefixed.keySet());
                    return NodeResult.ok(nextNodeId, prefixed);
                });
    }

}
