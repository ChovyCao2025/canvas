package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 开始节点：流程图入口标记，无实际逻辑，直接触发下游。
 * 触发器类型（is_trigger=1），无入边，无额外配置。
 */
@Component
@NodeHandlerType("START")
public class StartHandler implements NodeHandler {

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String nextNodeId = (String) config.get("nextNodeId");
        return Mono.just(NodeResult.ok(nextNodeId, Map.of()));
    }

    @Override public boolean isBenefitNode() { return false; }
    @Override public boolean isReachNode()   { return false; }
}
