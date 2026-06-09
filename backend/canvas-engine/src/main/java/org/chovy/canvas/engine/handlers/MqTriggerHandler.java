package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.domain.meta.MqMessageDefinitionService;
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
 * MQ 触发节点：校验消息内容 validateRules，通过则将 payload 写入上下文。
 */
@Component
@NodeHandlerType("MQ_TRIGGER")
public class MqTriggerHandler implements NodeHandler {

    /** MQ 消息定义服务，用于按 messageCode 解析实际订阅 topic。 */
    private final MqMessageDefinitionService mqMessageDefinitionService;

    /**
     * 创建 MqTriggerHandler 实例并注入 engine.handlers 场景依赖。
     * @param mqMessageDefinitionService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MqTriggerHandler(MqMessageDefinitionService mqMessageDefinitionService) {
        this.mqMessageDefinitionService = mqMessageDefinitionService;
    }

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
        // 消息入站解析与 payload 合并已在 Trigger 消费链路完成
        // validateResult=true 时才执行配置的规则校验
        Boolean validateResult = (Boolean) config.get(MapFieldKeys.VALIDATE_RESULT);
        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get(MapFieldKeys.VALIDATE_RULES);
        String nextNodeId = (String) config.get(MapFieldKeys.NEXT_NODE_ID);

        // 校验消息内容
        if (Boolean.TRUE.equals(validateResult) && rules != null) {
            for (Map<String, Object> rule : rules) {
                if (!IfConditionHandler.evaluate(rule, ctx)) {
                    // 任一入站规则失败即阻断流程，避免错误消息污染下游上下文。
                    return Mono.just(NodeResult.fail("MQ 消息校验不通过: " + rule.get(MapFieldKeys.FIELD)));
                }
            }
        }

        // triggerPayload 已在 CanvasExecutionService 合并到 ctx，这里只需输出
        Map<String, Object> output = new HashMap<>(ctx.getTriggerPayload());
        return Mono.just(NodeResult.ok(nextNodeId, output));
    }

    /**
     * Resolves the actual MQ topic string from node config.
     * Tries messageCodeKey first (new format after V29), falls back to topicKey (legacy).
     */
    public String resolveTopic(Map<String, Object> config) {
        return mqMessageDefinitionService.resolveTopic(config);
    }
}
