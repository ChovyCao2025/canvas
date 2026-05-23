package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * IF 判断节点：所有 rules 均满足 → successNodeId，否则 → failNodeId
 *
 * 规则语义：
 * - 多条规则之间是 AND 关系（allMatch）；
 * - 每条规则支持上下文字段与常量比较。
 */
@Component
@NodeHandlerType("IF_CONDITION")
public class IfConditionHandler implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("rules");
        String successNodeId = (String) config.get(MapFieldKeys.SUCCESS_NODE_ID);
        String failNodeId    = (String) config.get(MapFieldKeys.FAIL_NODE_ID);

        // rules 为空时按“无约束”为 true，直接走 success 分支
        boolean allMatch = rules == null || rules.stream().allMatch(r -> evaluate(r, ctx));
        return Mono.just(NodeResult.ifResult(allMatch, successNodeId, failNodeId));
    }

    static boolean evaluate(Map<String, Object> rule, ExecutionContext ctx) {
        return ConditionEvaluator.evaluate(rule, ctx);
    }
}
