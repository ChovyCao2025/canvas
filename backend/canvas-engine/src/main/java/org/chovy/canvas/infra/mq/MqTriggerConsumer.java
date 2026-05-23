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
import org.chovy.canvas.domain.execution.CanvasMqTriggerRejected;
import org.chovy.canvas.domain.execution.CanvasMqTriggerRejectedMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.request.CanvasExecutionRequestService;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.infra.redis.TriggerRouteService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
        consumeMode = ConsumeMode.ORDERLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadNumber = 20
)
public class MqTriggerConsumer implements RocketMQListener<MessageExt> {

    private final ObjectMapper objectMapper;
    private final TriggerRouteService routeService;
    private final CanvasDisruptorService disruptorService;
    private final CanvasExecutionRequestService requestService;
    private final CanvasMqTriggerRejectedMapper rejectedMapper;
    private final CanvasMetrics metrics;

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
            recordRejected("INVALID_BODY", tag);
            storeRejected(msgId, tag, "INVALID_BODY", e.getMessage(), body);
            return;
        }
        try {
            validateMessage(triggerMessage);
        } catch (IllegalArgumentException e) {
            recordRejected("INVALID_MESSAGE", tag);
            storeRejected(msgId, tag, "INVALID_MESSAGE", e.getMessage(), body);
            return;
        }

        Set<String> canvasIds = routeService.getCanvasByMqTopic(tag);
        if (canvasIds.isEmpty()) {
            log.warn("[MQ_CONSUMER] tag={} 无匹配画布，丢弃消息 msgId={}", tag, msgId);
            return;
        }

        for (String canvasIdStr : canvasIds) {
            Long canvasId = parseCanvasId(canvasIdStr, tag);
            if (canvasId == null) {
                continue;
            }
            String requestId = requestService.enqueue(
                    canvasId,
                    triggerMessage.getUserId(),
                    TriggerType.MQ,
                    NodeType.MQ_TRIGGER,
                    tag,
                    triggerMessage.getPayload(),
                    msgId
            );
            disruptorService.publishRequest(requestId);
            log.info("[MQ_CONSUMER] 投递到 Disruptor canvasId={} userId={} tag={}",
                    canvasId, triggerMessage.getUserId(), tag);
        }
    }

    private Long parseCanvasId(String canvasIdStr, String tag) {
        try {
            long canvasId = Long.parseLong(canvasIdStr);
            if (canvasId <= 0) {
                throw new NumberFormatException("non-positive canvasId");
            }
            return canvasId;
        } catch (RuntimeException e) {
            log.warn("[MQ_CONSUMER] 跳过非法路由 canvasId={} tag={}", canvasIdStr, tag);
            recordRouteRejected("INVALID_CANVAS_ID", tag);
            return null;
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

    private void recordRejected(String reason, String tag) {
        try {
            metrics.recordMqTriggerRejected(reason, tag);
        } catch (RuntimeException ignored) {
            // Metrics must not affect RocketMQ retry semantics.
        }
    }

    private void recordRouteRejected(String reason, String tag) {
        try {
            metrics.recordMqRouteRejected(reason, tag);
        } catch (RuntimeException ignored) {
            // Metrics must not affect RocketMQ retry semantics.
        }
    }

    private void storeRejected(String msgId, String tag, String reason, String errorMsg, String body) {
        try {
            CanvasMqTriggerRejected rejected = new CanvasMqTriggerRejected();
            rejected.setMsgId(trim(msgId, 255));
            rejected.setTag(trim(tag, 128));
            rejected.setReason(trim(reason, 64));
            rejected.setErrorMsg(trim(errorMsg, 500));
            rejected.setBody(trim(body, 4000));
            rejected.setCreatedAt(LocalDateTime.now());
            rejectedMapper.insert(rejected);
        } catch (RuntimeException e) {
            log.error("[MQ_CONSUMER] rejected 消息落库失败 msgId={} reason={}: {}",
                    msgId, reason, e.getMessage(), e);
        }
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
