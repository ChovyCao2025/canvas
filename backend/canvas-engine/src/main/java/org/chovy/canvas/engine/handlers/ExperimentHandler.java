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
 * 实验分流节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.EXPERIMENT)
public class ExperimentHandler implements NodeHandler {
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        List<Map<String, Object>> variants = (List<Map<String, Object>>) config.get(MapFieldKeys.VARIANTS);
        String experimentKey = String.valueOf(config.getOrDefault(
                MapFieldKeys.EXPERIMENT_KEY,
                config.getOrDefault(MapFieldKeys.EXPERIMENT_NAME, "experiment")));
        boolean stable = !MapFieldKeys.RANDOM.equalsIgnoreCase(String.valueOf(
                config.getOrDefault(MapFieldKeys.ALLOCATION_STRATEGY, MapFieldKeys.CONSISTENT)));
        Map<String, Object> chosen = WeightedChoice.choose(variants, ctx.getUserId() + ":" + experimentKey, stable);
        if (chosen == null) return Mono.just(NodeResult.terminal(Map.of()));
        String variantId = String.valueOf(chosen.getOrDefault(MapFieldKeys.VARIANT_ID, chosen.getOrDefault(MapFieldKeys.ID, "variant")));
        Object next = chosen.get(MapFieldKeys.NEXT_NODE_ID);
        String nextNodeId = next == null ? null : next.toString();
        return Mono.just(NodeResult.routed(variantId, nextNodeId, Map.of(
                MapFieldKeys.EXPERIMENT_KEY, experimentKey,
                MapFieldKeys.VARIANT_ID, variantId,
                MapFieldKeys.IS_CONTROL, Boolean.TRUE.equals(chosen.get(MapFieldKeys.IS_CONTROL))
        )));
    }
}
