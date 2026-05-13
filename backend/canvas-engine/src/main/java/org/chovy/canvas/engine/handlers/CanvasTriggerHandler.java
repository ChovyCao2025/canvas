package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.infra.cache.CanvasConfigCache;
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

    private final CanvasMapper      canvasMapper;
    private final CanvasConfigCache configCache;
    private final DagEngine         dagEngine;

    public CanvasTriggerHandler(CanvasMapper canvasMapper,
                                CanvasConfigCache configCache,
                                @Lazy DagEngine dagEngine) {
        this.canvasMapper = canvasMapper;
        this.configCache  = configCache;
        this.dagEngine    = dagEngine;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        Object targetId = config.get("targetCanvasId");
        if (targetId == null) return Mono.just(NodeResult.fail("CANVAS_TRIGGER 缺少 targetCanvasId"));
        Long targetCanvasId = Long.parseLong(String.valueOf(targetId));

        String invokeMode = (String) config.getOrDefault("invokeMode", "SYNC");
        String nextNodeId = (String) config.get("nextNodeId");
        Map<String, Object> paramMapping =
                (Map<String, Object>) config.getOrDefault("paramMapping", Map.of());

        if (ctx.getCallStack().contains(targetCanvasId)) {
            return Mono.just(NodeResult.fail("CANVAS_TRIGGER 检测到循环调用: " + targetCanvasId));
        }

        Canvas target = canvasMapper.selectById(targetCanvasId);
        if (target == null || target.getStatus() != 1) {
            return Mono.just(NodeResult.fail("目标画布未发布: " + targetCanvasId));
        }

        ExecutionContext childCtx = new ExecutionContext();
        childCtx.setExecutionId(ctx.getExecutionId() + ":sub:" + UUID.randomUUID().toString().substring(0, 8));
        childCtx.setCanvasId(targetCanvasId);
        childCtx.setVersionId(target.getPublishedVersionId());
        childCtx.setUserId(ctx.getUserId());
        childCtx.setTriggerType("CANVAS_TRIGGER");

        paramMapping.forEach((childKey, parentKeyObj) -> {
            String parentKey = String.valueOf(parentKeyObj).replace("ctx.", "");
            Object val = ctx.getContextValue(parentKey);
            if (val != null) childCtx.getTriggerPayload().put(childKey, val);
        });

        childCtx.getCallStack().addAll(ctx.getCallStack());
        childCtx.getCallStack().add(ctx.getCanvasId());

        DagGraph childGraph = configCache.get(targetCanvasId, target.getPublishedVersionId());
        String triggerNodeId = childGraph.entryNodes().isEmpty() ? null : childGraph.entryNodes().get(0);
        if (triggerNodeId == null) return Mono.just(NodeResult.fail("目标画布无触发器节点"));

        if ("ASYNC".equals(invokeMode)) {
            dagEngine.execute(childGraph, triggerNodeId, childCtx).subscribe();
            log.info("[CANVAS_TRIGGER] ASYNC 触发子画布 canvasId={}", targetCanvasId);
            return Mono.just(NodeResult.ok(nextNodeId, Map.of()));
        }

        // SYNC：直接 flatMap 子执行链，无需 block()
        return dagEngine.execute(childGraph, triggerNodeId, childCtx)
                .map(result -> {
                    log.info("[CANVAS_TRIGGER] SYNC 子画布完成 canvasId={}", targetCanvasId);
                    return NodeResult.ok(nextNodeId, result != null ? result : Map.of());
                })
                .onErrorResume(e -> {
                    log.error("[CANVAS_TRIGGER] 执行失败: {}", e.getMessage());
                    return Mono.just(NodeResult.fail("子画布执行失败: " + e.getMessage()));
                });
    }
}
