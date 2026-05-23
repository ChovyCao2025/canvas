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
import org.chovy.canvas.domain.notification.NotificationEventService;
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

    private static final int ALERT_CONTENT_LIMIT = 900;

    private final ObjectMapper objectMapper;
    private final TriggerRouteService routeService;
    private final CanvasDisruptorService disruptorService;
    private final NotificationEventService notificationEventService;

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
            notificationEventService.systemAlert(
                    "MQ_TRIGGER_PARSE_FAILED",
                    "MQ 触发消息解析失败",
                    trim("tag=" + tag + " msgId=" + msgId + " error=" + e.getMessage() + " body=" + body),
                    "/mq-config",
                    "MQ_TRIGGER",
                    msgId,
                    "mq:parse:" + msgId,
                    null);
            throw new IllegalArgumentException("Invalid MQ trigger message body: " + e.getMessage(), e);
        }
        try {
            validateMessage(triggerMessage);
        } catch (IllegalArgumentException e) {
            notificationEventService.systemAlert(
                    "MQ_TRIGGER_VALIDATE_FAILED",
                    "MQ 触发消息校验失败",
                    trim("tag=" + tag + " msgId=" + msgId + " error=" + e.getMessage()),
                    "/mq-config",
                    "MQ_TRIGGER",
                    msgId,
                    "mq:validate:" + msgId,
                    null);
            throw e;
        }

        Set<String> canvasIds = routeService.getCanvasByMqTopic(tag);
        if (canvasIds.isEmpty()) {
            log.warn("[MQ_CONSUMER] tag={} 无匹配画布，丢弃消息 msgId={}", tag, msgId);
            notificationEventService.systemAlert(
                    "MQ_TRIGGER_NO_ROUTE",
                    "MQ 触发无匹配画布",
                    "tag=" + tag + " msgId=" + msgId + " 未匹配到已发布画布",
                    "/mq-config",
                    "MQ_TRIGGER",
                    tag,
                    "mq:no-route:" + tag,
                    null);
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

    private String trim(String value) {
        if (value == null || value.length() <= ALERT_CONTENT_LIMIT) {
            return value;
        }
        return value.substring(0, ALERT_CONTENT_LIMIT);
    }
}
