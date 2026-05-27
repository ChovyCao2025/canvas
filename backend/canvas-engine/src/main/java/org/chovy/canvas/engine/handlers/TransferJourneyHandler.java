package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 旅程转移节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.TRANSFER_JOURNEY)
public class TransferJourneyHandler implements NodeHandler {
    private final CanvasExecutionService executionService;

    public TransferJourneyHandler(@Lazy CanvasExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        Object target = config.get("targetJourneyId");
        if (target == null) {
            return Mono.just(NodeResult.fail("TRANSFER_JOURNEY 必须配置 targetJourneyId"));
        }
        Long targetJourneyId = Long.parseLong(target.toString());
        Map<String, Object> payload = new HashMap<>();
        if (Boolean.TRUE.equals(config.get("carryContext"))) {
            payload.putAll(ctx.getFlatContext());
            payload.putAll(ctx.getTriggerPayload());
        }
        payload.put(MapFieldKeys.SOURCE_EXECUTION_ID, ctx.getExecutionId());
        executionService.trigger(
                        targetJourneyId,
                        ctx.getUserId(),
                        TriggerType.TRANSFER_JOURNEY,
                        NodeType.DIRECT_CALL,
                        null,
                        payload,
                        ctx.getExecutionId() + ":transfer:" + targetJourneyId,
                        false)
                .subscribe();
        return Mono.just(NodeResult.ok(string(config, "nextNodeId", null),
                Map.of(MapFieldKeys.TRANSFERRED_JOURNEY_ID, targetJourneyId)));
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
