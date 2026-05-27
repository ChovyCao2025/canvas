package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 人群触发节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.AUDIENCE_TRIGGER)
public class AudienceTriggerHandler implements NodeHandler {
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String expectedSegment = string(config, "segmentId", null);
        String triggerOn = string(config, "triggerOn", "ENTER");
        Object actualSegment = ctx.getContextValue("segmentId");
        Object actualAction = ctx.getContextValue("audienceAction");
        if (expectedSegment != null && actualSegment != null && !expectedSegment.equals(actualSegment.toString())) {
            return Mono.just(NodeResult.terminal(Map.of(MapFieldKeys.AUDIENCE_MATCHED, false)));
        }
        if (actualAction != null && !triggerOn.equalsIgnoreCase(actualAction.toString())) {
            return Mono.just(NodeResult.terminal(Map.of(MapFieldKeys.AUDIENCE_MATCHED, false)));
        }
        return Mono.just(NodeResult.ok(string(config, "nextNodeId", null), Map.of(MapFieldKeys.AUDIENCE_MATCHED, true)));
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
