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

/**
 * Generic split node replacing AB, random, and experiment split variants.
 */
@Component
@NodeHandlerType(NodeType.SPLIT)
public class SplitHandler implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        List<Map<String, Object>> branches = (List<Map<String, Object>>) config.get(MapFieldKeys.BRANCHES);
        boolean stable = !MapFieldKeys.RANDOM.equalsIgnoreCase(String.valueOf(
                config.getOrDefault(MapFieldKeys.ALLOCATION_STRATEGY, MapFieldKeys.CONSISTENT)));
        String splitKey = String.valueOf(config.getOrDefault("splitKey", ctx.getCanvasId()));
        Map<String, Object> chosen = WeightedChoice.choose(branches, ctx.getUserId() + ":" + splitKey, stable);
        if (chosen == null) {
            return Mono.just(NodeResult.terminal(Map.of()));
        }

        String branchId = string(chosen.getOrDefault("branchId", chosen.getOrDefault(MapFieldKeys.ID, "branch")));
        String nextNodeId = string(chosen.get(MapFieldKeys.NEXT_NODE_ID));
        return Mono.just(NodeResult.routed(
                "branch-" + branchId,
                nextNodeId,
                Map.of("splitBranch", branchId)
        ));
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }
}
