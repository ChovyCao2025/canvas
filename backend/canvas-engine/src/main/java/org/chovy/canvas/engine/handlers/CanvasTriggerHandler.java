package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 触发子画布节点（设计文档 18.3 节）。
 *
 * SYNC 模式：等待子画布执行完成，将输出写入父上下文。
 * ASYNC 模式：fire-and-forget，父画布立即继续。
 * 防循环：子画布 ID 不能出现在 ctx.callStack 中。
 */
@Component
@Slf4j
@NodeHandlerType("CANVAS_TRIGGER")
public class CanvasTriggerHandler implements NodeHandler {

    /** 画布数据访问器，用于校验目标子画布发布状态。 */
    private final CanvasMapper      canvasMapper;

    /** 画布配置缓存，用于加载目标子画布 DAG。 */
    private final CanvasConfigCache configCache;

    /** DAG 执行引擎，用于同步或异步启动子画布。 */
    private final DagEngine         dagEngine;

    /**
     * 构造 CanvasTriggerHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param canvasMapper canvasMapper 画布相关对象或标识
     * @param configCache configCache 方法执行所需的业务参数
     * @param dagEngine dagEngine 方法执行所需的业务参数
     */
    public CanvasTriggerHandler(CanvasMapper canvasMapper,
                                CanvasConfigCache configCache,
                                @Lazy DagEngine dagEngine) {
        this.canvasMapper = canvasMapper;
        this.configCache  = configCache;
        this.dagEngine    = dagEngine;
    }

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // 1) 读取目标子画布 ID
        Object targetId = config.get("targetCanvasId");
        if (targetId == null) return Mono.just(NodeResult.fail("CANVAS_TRIGGER 缺少 targetCanvasId"));
        Long targetCanvasId = Long.parseLong(String.valueOf(targetId));

        // 2) 读取调用模式和参数映射规则
        String invokeMode = (String) config.getOrDefault("invokeMode", "SYNC");
        String nextNodeId = (String) config.get(MapFieldKeys.NEXT_NODE_ID);
        Map<String, Object> paramMapping =
                (Map<String, Object>) config.getOrDefault("paramMapping", Map.of());

        // 3) 防止循环调用导致无限递归（A -> B -> A）
        if (ctx.getCallStack().contains(targetCanvasId)) {
            return Mono.just(NodeResult.fail("CANVAS_TRIGGER 检测到循环调用: " + targetCanvasId));
        }

        // 4) 校验目标画布存在且已发布（只有发布版本可被调用）
        CanvasDO target = canvasMapper.selectById(targetCanvasId);
        if (target == null || target.getStatus() != 1) {
            return Mono.just(NodeResult.fail("目标画布未发布: " + targetCanvasId));
        }

        // 5) 构造子执行上下文，继承用户与调用链信息
        ExecutionContext childCtx = new ExecutionContext();
        childCtx.setExecutionId(ctx.getExecutionId() + ":sub:" + UUID.randomUUID().toString().substring(0, 8));
        childCtx.setCanvasId(targetCanvasId);
        childCtx.setVersionId(target.getPublishedVersionId());
        childCtx.setUserId(ctx.getUserId());
        childCtx.setTriggerType("CANVAS_TRIGGER");

        // 6) 按映射规则把父上下文字段写入子触发载荷
        paramMapping.forEach((childKey, parentKeyObj) -> {
            String parentKey = String.valueOf(parentKeyObj).replace("ctx.", "");
            Object val = ctx.getContextValue(parentKey);
            if (val != null) childCtx.getTriggerPayload().put(childKey, val);
        });

        // 7) 追加调用栈（用于下一层继续防循环）
        childCtx.getCallStack().addAll(ctx.getCallStack());
        childCtx.getCallStack().add(ctx.getCanvasId());

        // 8) 获取目标发布图并定位入口触发节点
        DagGraph childGraph = configCache.get(targetCanvasId, target.getPublishedVersionId());
        String triggerNodeId = childGraph.entryNodes().isEmpty() ? null : childGraph.entryNodes().get(0);
        if (triggerNodeId == null) return Mono.just(NodeResult.fail("目标画布无触发器节点"));

        if ("ASYNC".equals(invokeMode)) {
            // ASYNC 模式 fire-and-forget，父流程立即继续，不合并子画布输出。
            dagEngine.execute(childGraph, triggerNodeId, childCtx).subscribe();
            log.info("[CANVAS_TRIGGER] ASYNC 触发子画布 canvasId={}", targetCanvasId);
            return Mono.just(NodeResult.ok(nextNodeId, Map.of()));
        }

        // SYNC：直接 flatMap 子执行链，无需 block()
        return dagEngine.execute(childGraph, triggerNodeId, childCtx)
                .map(result -> {
                    log.info("[CANVAS_TRIGGER] SYNC 子画布完成 canvasId={}", targetCanvasId);
                    // SYNC 模式把子画布最终输出合并回父流程当前节点输出。
                    return NodeResult.ok(nextNodeId, result != null ? result : Map.of());
                })
                .onErrorResume(e -> {
                    log.error("[CANVAS_TRIGGER] 执行失败: {}", e.getMessage());
                    return Mono.just(NodeResult.fail("子画布执行失败: " + e.getMessage()));
                });
    }
}
