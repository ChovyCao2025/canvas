package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@NodeHandlerType(NodeType.RECOMMENDATION)
public class RecommendationHandler implements NodeHandler {
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        List<Object> items = (List<Object>) config.getOrDefault("fallbackItems", List.of());
        int limit = config.get("limit") instanceof Number number ? number.intValue() : items.size();
        List<Object> selected = items.stream().limit(Math.max(0, limit)).toList();
        return Mono.just(NodeResult.routed("success", string(config, "successNodeId", string(config, "nextNodeId", null)),
                Map.of(MapFieldKeys.RECOMMENDATIONS, selected)));
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
