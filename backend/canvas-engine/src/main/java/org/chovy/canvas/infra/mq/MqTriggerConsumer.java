package org.chovy.canvas.infra.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.infra.redis.TriggerRouteService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * RocketMQ MQ 触发消费者。
 *
 * 消费链路：RocketMQ CANVAS_MQ_TRIGGER -> 按 Tag 路由 -> Disruptor Ring Buffer -> DagEngine。
 * 发布到 Disruptor 或路由读取失败时向上抛出，让 RocketMQ 重新消费。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${canvas.mq.topic:CANVAS_MQ_TRIGGER}",
        consumerGroup = "${rocketmq.consumer.group:GID_CANVAS_ENGINE}",
        selectorType = SelectorType.TAG,
        selectorExpression = "*",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadNumber = 20
)
public class MqTriggerConsumer implements RocketMQListener<MessageExt> {

    private final ObjectMapper objectMapper;
    private final TriggerRouteService routeService;
    private final CanvasDisruptorService disruptorService;

    @Override
    public void onMessage(MessageExt message) {
        String tag = message.getTags();
        String msgId = message.getMsgId();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        log.info("[MQ_CONSUMER] 收到消息 tag={} msgId={}", tag, msgId);

        MqTriggerMessage triggerMessage;
        try {
            triggerMessage = objectMapper.readValue(body, MqTriggerMessage.class);
        } catch (Exception e) {
            log.error("[MQ_CONSUMER] 消息体解析失败 msgId={} body={}: {}", msgId, body, e.getMessage());
            throw new IllegalArgumentException("Invalid MQ trigger message body: " + e.getMessage(), e);
        }
        validateMessage(triggerMessage);

        Set<String> canvasIds = routeService.getCanvasByMqTopic(tag);
        if (canvasIds.isEmpty()) {
            log.warn("[MQ_CONSUMER] tag={} 无匹配画布，丢弃消息 msgId={}", tag, msgId);
            return;
        }

        for (String canvasIdStr : canvasIds) {
            Long canvasId = Long.parseLong(canvasIdStr);
            disruptorService.publish(
                    canvasId,
                    triggerMessage.getUserId(),
                    TriggerType.MQ,
                    NodeType.MQ_TRIGGER,
                    tag,
                    triggerMessage.getPayload(),
                    msgId
            );
            log.info("[MQ_CONSUMER] 投递到 Disruptor canvasId={} userId={} tag={}",
                    canvasId, triggerMessage.getUserId(), tag);
        }
    }

    private void validateMessage(MqTriggerMessage message) {
        if (message.getUserId() == null || message.getUserId().isBlank()) {
            throw new IllegalArgumentException("Invalid MQ trigger message body: userId is required");
        }
        if (message.getMessageCode() == null || message.getMessageCode().isBlank()) {
            throw new IllegalArgumentException("Invalid MQ trigger message body: messageCode is required");
        }
        if (message.getPayload() == null) {
            throw new IllegalArgumentException("Invalid MQ trigger message body: payload is required");
        }
    }
}
