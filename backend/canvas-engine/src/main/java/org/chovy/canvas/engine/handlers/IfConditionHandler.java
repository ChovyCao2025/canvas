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

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
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

    /**
     * 执行 evaluate 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param rule rule 方法执行所需的业务参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 判断结果，true 表示校验通过或条件成立
     */
    static boolean evaluate(Map<String, Object> rule, ExecutionContext ctx) {
        return ConditionEvaluator.evaluate(rule, ctx);
    }
}
