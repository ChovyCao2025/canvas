package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.domain.meta.MqMessageDefinition;
import org.chovy.canvas.domain.meta.MqMessageDefinitionMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    private MqMessageDefinitionMapper mqMessageDefinitionMapper;

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
        String messageCode = (String) config.get(MapFieldKeys.MESSAGE_CODE_KEY);
        if (messageCode != null && mqMessageDefinitionMapper != null) {
            MqMessageDefinition def = mqMessageDefinitionMapper.selectOne(
                new LambdaQueryWrapper<MqMessageDefinition>()
                    .eq(MqMessageDefinition::getMessageCode, messageCode)
                    .eq(MqMessageDefinition::getEnabled, 1));
            if (def != null) return def.getTopic();
        }
        // Backward-compat: old canvases store topicKey directly
        return (String) config.getOrDefault(MapFieldKeys.TOPIC_KEY, "");
    }
}
