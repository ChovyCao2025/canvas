package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.domain.meta.MqMessageDefinition;
import org.chovy.canvas.domain.meta.MqMessageDefinitionMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.infra.mq.MqTriggerMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发送 MQ 消息节点：将业务消息发送到 RocketMQ CANVAS_MQ_TRIGGER topic。
 * Tag = MqMessageDefinition.topic，供消费端路由到对应画布。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@NodeHandlerType("SEND_MQ")
public class SendMqHandler implements NodeHandler {

    private final RocketMQTemplate rocketMQTemplate;
    private final MqMessageDefinitionMapper mqMapper;

    @Value("${canvas.mq.topic:CANVAS_MQ_TRIGGER}")
    private String mqTopic;

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String messageCodeKey = (String) config.get("messageCodeKey");
        String nextNodeId     = (String) config.get("nextNodeId");

        if (messageCodeKey == null || messageCodeKey.isBlank()) {
            return Mono.just(NodeResult.fail("SEND_MQ: messageCodeKey 未配置"));
        }

        MqMessageDefinition def = mqMapper.selectOne(
                new LambdaQueryWrapper<MqMessageDefinition>()
                        .eq(MqMessageDefinition::getMessageCode, messageCodeKey)
                        .eq(MqMessageDefinition::getEnabled, 1));
        if (def == null) {
            return Mono.just(NodeResult.fail("SEND_MQ: 找不到消息定义 messageCode=" + messageCodeKey));
        }

        List<Map<String, Object>> paramsList =
                (List<Map<String, Object>>) config.getOrDefault("params", List.of());
        Map<String, Object> payload = new HashMap<>();
        for (Map<String, Object> param : paramsList) {
            String key = (String) param.get("key");
            Object value = param.get("value");
            if (value instanceof String stringValue) {
                String normalized = stringValue.startsWith("$${") ? stringValue.substring(1) : stringValue;
                if (normalized.startsWith("${") && normalized.endsWith("}")) {
                    value = ctx.getContextValue(normalized.substring(2, normalized.length() - 1));
                }
            }
            if (key != null) {
                payload.put(key, value);
            }
        }

        MqTriggerMessage message = new MqTriggerMessage(ctx.getUserId(), messageCodeKey, payload);
        String destination = mqTopic + ":" + def.getTopic();

        return Mono.fromRunnable(() -> {
                    rocketMQTemplate.syncSend(destination, message);
                    log.info("[SEND_MQ] 发送成功 destination={} userId={}", destination, ctx.getUserId());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(NodeResult.ok(nextNodeId, Map.of("mqSent", true)))
                .onErrorResume(e -> {
                    log.error("[SEND_MQ] 发送失败 destination={}: {}", destination, e.getMessage());
                    return Mono.just(NodeResult.fail("SEND_MQ: 消息发送失败: " + e.getMessage()));
                });
    }
}
