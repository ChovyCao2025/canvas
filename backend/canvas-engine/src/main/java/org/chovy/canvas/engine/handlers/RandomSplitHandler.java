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
@NodeHandlerType(NodeType.RANDOM_SPLIT)
public class RandomSplitHandler implements NodeHandler {
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        List<Map<String, Object>> paths = (List<Map<String, Object>>) config.get("paths");
        boolean stable = !"RANDOM".equalsIgnoreCase(String.valueOf(config.getOrDefault("allocationStrategy", "CONSISTENT")));
        Map<String, Object> chosen = WeightedChoice.choose(paths, ctx.getUserId() + ":" + ctx.getCanvasId(), stable);
        if (chosen == null) return Mono.just(NodeResult.terminal(Map.of()));
        String pathId = String.valueOf(chosen.getOrDefault("pathId", chosen.getOrDefault("id", "path")));
        Object next = chosen.get("nextNodeId");
        String nextNodeId = next == null ? null : next.toString();
        return Mono.just(NodeResult.routed(pathId, nextNodeId, Map.of("splitPath", pathId)));
    }
}
