package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 端内行为触发节点：评估 AND/OR 策略组合。
 */
@Component
@NodeHandlerType("BEHAVIOR_IN_APP")
public class BehaviorInAppHandler implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String relation = (String) config.getOrDefault("strategyRelation", "OR");
        List<Map<String, Object>> strategies = (List<Map<String, Object>>) config.get("strategies");
        String nextNodeId = (String) config.get("nextNodeId");

        if (strategies == null || strategies.isEmpty()) {
            return Mono.just(NodeResult.ok(nextNodeId, Map.of()));
        }

        boolean matched = "OR".equals(relation)
                ? strategies.stream().anyMatch(s -> evaluateStrategy(s, ctx))
                : strategies.stream().allMatch(s -> evaluateStrategy(s, ctx));

        if (!matched) return NodeResult.fail("行为策略条件不满足");

        return Mono.just(NodeResult.ok(nextNodeId, new HashMap<>(ctx.getTriggerPayload())));
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateStrategy(Map<String, Object> strategy, ExecutionContext ctx) {
        // 从行为数据中评估策略参数（简化：从 triggerPayload 取值与 params 比较）
        Map<String, Object> params = (Map<String, Object>) strategy.get("params");
        if (params == null) return true;
        // 实际评估逻辑由行为策略系统处理，此处作为触达验证
        return true;
    }
}
