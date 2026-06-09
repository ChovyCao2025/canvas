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

    /**
     * 执行分流节点：按权重和分配策略选择一个分支，并把命中的分支 ID 写入上下文。
     *
     * <p>consistent 策略会使用用户 ID 与 splitKey 生成稳定选择，random 策略每次重新随机。
     * 命中分支后路由到该分支的 nextNodeId；未配置可用分支时返回 terminal，不产生外部副作用。</p>
     *
     * @param config 节点配置，包含 branches、allocationStrategy、splitKey 和各分支 nextNodeId
     * @param ctx 执行上下文，提供用户 ID 和画布 ID 用于稳定分桶
     * @return 分支路由结果，输出字段 {@code splitBranch} 表示实际命中的分支
     */
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

    /**
     * 将对象转换为字符串。
     *
     * @param value 原始值
     * @return 字符串值，null 保持为 null
     */
    private String string(Object value) {
        return value == null ? null : value.toString();
    }
}
