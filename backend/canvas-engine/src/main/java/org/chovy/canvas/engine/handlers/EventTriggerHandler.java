package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 事件触发节点（EVENT_TRIGGER）。
 *
 * 画布收到业务事件后，从 START 节点路由到此节点开始执行。
 * 触发 payload（eventCode、attributes 等）已由执行引擎注入 ctx.triggerPayload，
 * 此节点直接透传并路由到下游。
 */
@Component
@NodeHandlerType("EVENT_TRIGGER")
public class EventTriggerHandler implements NodeHandler {

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String expectedEvent = (String) config.get("eventCode");
        Object actualEvent = ctx.getContextValue("eventCode");
        if (expectedEvent != null && actualEvent != null && !expectedEvent.equals(actualEvent.toString())) {
            return Mono.just(NodeResult.terminal(Map.of("eventMatched", false)));
        }
        String nextNodeId = (String) config.get("nextNodeId");
        return Mono.just(NodeResult.ok(nextNodeId, Map.of("eventMatched", true)));
    }
}
