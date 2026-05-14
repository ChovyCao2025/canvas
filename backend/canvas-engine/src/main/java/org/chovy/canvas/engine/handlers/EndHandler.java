package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 结束节点：流程图出口标记，终止节点（is_terminal=1），无出边，无额外逻辑。
 */
@Component
@NodeHandlerType("END")
public class EndHandler implements NodeHandler {

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        return Mono.just(NodeResult.terminal(Map.of()));
    }

    @Override public boolean isBenefitNode() { return false; }
    @Override public boolean isReachNode()   { return false; }
}
