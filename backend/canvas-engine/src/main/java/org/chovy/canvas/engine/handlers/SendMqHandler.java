package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
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
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String rawMessageCodeKey = (String) config.get("messageCodeKey");
        String messageCodeKey = rawMessageCodeKey != null ? rawMessageCodeKey.trim() : null;
        String nextNodeId = (String) config.get("nextNodeId");

        if (messageCodeKey == null || messageCodeKey.isBlank()) {
            return Mono.just(NodeResult.fail("SEND_MQ: messageCodeKey 未配置"));
        }

        return Mono.fromCallable(() -> {
                    MqMessageDefinition def = mqMapper.selectOne(
                            new LambdaQueryWrapper<MqMessageDefinition>()
                                    .eq(MqMessageDefinition::getMessageCode, messageCodeKey)
                                    .eq(MqMessageDefinition::getEnabled, 1));
                    if (def == null) {
                        return NodeResult.fail("SEND_MQ: 找不到消息定义 messageCode=" + messageCodeKey);
                    }
                    if (def.getTopic() == null || def.getTopic().isBlank()) {
                        return NodeResult.fail("SEND_MQ: 消息定义 topic 未配置 messageCode=" + messageCodeKey);
                    }

                    Map<String, Object> payload = buildPayload(config, ctx);
                    MqTriggerMessage message = new MqTriggerMessage(ctx.getUserId(), messageCodeKey, payload);
                    String destination = mqTopic + ":" + def.getTopic().trim();

                    SendResult sendResult = rocketMQTemplate.syncSend(destination, message);
                    if (sendResult == null || sendResult.getSendStatus() != SendStatus.SEND_OK) {
                        SendStatus status = sendResult != null ? sendResult.getSendStatus() : null;
                        throw new IllegalStateException("RocketMQ send status=" + status);
                    }

                    log.info("[SEND_MQ] 发送成功 destination={} userId={}", destination, ctx.getUserId());
                    return NodeResult.ok(nextNodeId, Map.of("mqSent", true));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("[SEND_MQ] 发送失败 messageCode={}: {}", messageCodeKey, e.getMessage(), e);
                    return Mono.just(NodeResult.fail("SEND_MQ: 消息发送失败: " + e.getMessage()));
                });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildPayload(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> payload = new HashMap<>();
        copyParams(payload, config.get("params"), ctx);
        copyParams(payload, config.get("inputParams"), ctx);
        return payload;
    }

    private void copyParams(Map<String, Object> payload, Object rawParams, ExecutionContext ctx) {
        if (rawParams == null) {
            return;
        }
        if (rawParams instanceof Map<?, ?> paramsMap) {
            paramsMap.forEach((key, value) -> {
                if (key != null) {
                    payload.put(String.valueOf(key), resolveValue(value, ctx));
                }
            });
            return;
        }
        if (rawParams instanceof List<?> paramsList) {
            for (Object item : paramsList) {
                if (!(item instanceof Map<?, ?> param)) {
                    throw new IllegalArgumentException("SEND_MQ: params 列表元素必须是对象");
                }
                Object key = param.get("key");
                if (key != null) {
                    payload.put(String.valueOf(key), resolveValue(param.get("value"), ctx));
                }
            }
            return;
        }
        throw new IllegalArgumentException("SEND_MQ: params 必须是对象或列表");
    }

    private Object resolveValue(Object value, ExecutionContext ctx) {
        if (value instanceof String stringValue) {
            String normalized = stringValue.startsWith("$${") ? stringValue.substring(1) : stringValue;
            if (normalized.startsWith("${") && normalized.endsWith("}")) {
                return ctx.getContextValue(normalized.substring(2, normalized.length() - 1));
            }
        }
        return value;
    }
}
