package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@NodeHandlerType(NodeType.AI_NEXT_BEST_ACTION)
public class AiNextBestActionHandler implements NodeHandler {
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String action = string(config, "fallbackAction", "continue");
        String route = string(config, "fallbackRoute", "fallback");
        String target = string(config, "fallbackNodeId", string(config, "nextNodeId", null));
        return Mono.just(NodeResult.routed(route, target, Map.of(
                "nextBestAction", action,
                "aiFallbackUsed", true
        )));
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
