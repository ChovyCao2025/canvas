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
 * MQ 触发节点：校验消息内容 validateRules，通过则将 payload 写入上下文。
 */
@Component
@NodeHandlerType("MQ_TRIGGER")
public class MqTriggerHandler implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        Boolean validateResult = (Boolean) config.get("validateResult");
        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("validateRules");
        String nextNodeId = (String) config.get("nextNodeId");

        // 校验消息内容
        if (Boolean.TRUE.equals(validateResult) && rules != null) {
            for (Map<String, Object> rule : rules) {
                if (!IfConditionHandler.evaluate(rule, ctx)) {
                    return Mono.just(NodeResult.fail("MQ 消息校验不通过: " + rule.get("field")));
                }
            }
        }

        // triggerPayload 已在 CanvasExecutionService 合并到 ctx，这里只需输出
        Map<String, Object> output = new HashMap<>(ctx.getTriggerPayload());
        return Mono.just(NodeResult.ok(nextNodeId, output));
    }
}
