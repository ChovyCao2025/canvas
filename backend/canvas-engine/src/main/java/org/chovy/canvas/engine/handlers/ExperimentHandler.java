package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@NodeHandlerType(NodeType.EXPERIMENT)
public class ExperimentHandler implements NodeHandler {
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        List<Map<String, Object>> variants = (List<Map<String, Object>>) config.get("variants");
        String experimentKey = String.valueOf(config.getOrDefault("experimentKey", config.getOrDefault("experimentName", "experiment")));
        boolean stable = !"RANDOM".equalsIgnoreCase(String.valueOf(config.getOrDefault("allocationStrategy", "CONSISTENT")));
        Map<String, Object> chosen = WeightedChoice.choose(variants, ctx.getUserId() + ":" + experimentKey, stable);
        if (chosen == null) return Mono.just(NodeResult.terminal(Map.of()));
        String variantId = String.valueOf(chosen.getOrDefault("variantId", chosen.getOrDefault("id", "variant")));
        Object next = chosen.get("nextNodeId");
        String nextNodeId = next == null ? null : next.toString();
        return Mono.just(NodeResult.routed(variantId, nextNodeId, Map.of(
                "experimentKey", experimentKey,
                "variantId", variantId,
                "isControl", Boolean.TRUE.equals(chosen.get("isControl"))
        )));
    }
}
