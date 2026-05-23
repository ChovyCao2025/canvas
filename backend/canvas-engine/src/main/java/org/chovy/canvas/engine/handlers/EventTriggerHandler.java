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
 * 与 MqTrigger/ScheduledTrigger 类似，属于轻量触发器节点。
 */
@Component
@NodeHandlerType("EVENT_TRIGGER")
public class EventTriggerHandler implements NodeHandler {

    /**
     * 事件触发节点本身不做业务判断，仅把流程推进到 nextNodeId。
     *
     * <p>事件有效性（事件编码、属性结构）由上游上报入口和事件定义校验。
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // 若节点配置了 eventCode，运行时再做一次轻量保护，避免错误事件推进流程。
        String expectedEvent = (String) config.get("eventCode");
        Object actualEvent = ctx.getContextValue("eventCode");
        if (expectedEvent != null && actualEvent != null && !expectedEvent.equals(actualEvent.toString())) {
            return Mono.just(NodeResult.terminal(Map.of("eventMatched", false)));
        }
        String nextNodeId = (String) config.get("nextNodeId");
        return Mono.just(NodeResult.ok(nextNodeId, Map.of("eventMatched", true)));
    }
}
