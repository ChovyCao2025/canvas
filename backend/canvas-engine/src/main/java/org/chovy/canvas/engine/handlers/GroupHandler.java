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

@Component
@NodeHandlerType(NodeType.GROUP)
public class GroupHandler implements NodeHandler {
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        return Mono.just(NodeResult.ok((String) config.get(MapFieldKeys.NEXT_NODE_ID), Map.of(MapFieldKeys.GROUP_PASSED, true)));
    }
}
