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
 * 条件选择器：按 branches 顺序匹配，命中则走对应 nextNodeId，全部不命中走 elseNodeId。
 *
 * <p>每个 branch 支持 AND / OR 两种条件组合关系，单条条件复用 IfConditionHandler 的比较逻辑。
 */
@Component
@NodeHandlerType("SELECTOR")
public class SelectorHandler implements NodeHandler {

    /**
     * 条件选择执行入口。
     *
     * <p>执行顺序严格按 `branches` 配置顺序，命中第一条即返回，不会继续评估后续分支。
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        List<Map<String, Object>> branches = (List<Map<String, Object>>) config.get("branches");
        String elseNodeId = (String) config.get("elseNodeId");

        // 分支按配置顺序短路匹配：命中第一条就停止
        if (branches != null) {
            for (int i = 0; i < branches.size(); i++) {
                Map<String, Object> branch = branches.get(i);
                if (branchMatches(branch, ctx)) {
                    String next = (String) branch.get("nextNodeId");
                    return Mono.just(NodeResult.ok(next, Map.of()));
                }
            }
        }

        // 全部不命中
        if (elseNodeId != null) return Mono.just(NodeResult.ok(elseNodeId, Map.of()));

        // 无 else：SUCCESS，流程自然结束
        return Mono.just(NodeResult.terminal(Map.of()));
    }

    @SuppressWarnings("unchecked")
    private boolean branchMatches(Map<String, Object> branch, ExecutionContext ctx) {
        String relation = (String) branch.getOrDefault("strategyRelation", "AND");
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) branch.get("conditions");
        // 空条件分支视为“默认命中”
        if (conditions == null || conditions.isEmpty()) return true;

        if ("OR".equals(relation)) {
            return conditions.stream().anyMatch(c -> IfConditionHandler.evaluate(c, ctx));
        }
        return conditions.stream().allMatch(c -> IfConditionHandler.evaluate(c, ctx));
    }
}
