package org.chovy.canvas.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasMqTriggerRejectedDO;
import org.chovy.canvas.dal.mapper.CanvasMqTriggerRejectedMapper;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.request.CanvasExecutionRequestService;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Mq Trigger Consumer RocketMQ 消息组件。
 *
 * <p>负责消费或重试画布触发消息，将外部 MQ 流量转换为内部执行请求。
 * <p>该层需要处理反序列化、幂等、异常降级和日志观测，避免消息异常扩散到执行引擎。
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

    private static final int ALERT_CONTENT_LIMIT = 900;

    private final ObjectMapper objectMapper;
    private final TriggerRouteService routeService;
    private final CanvasDisruptorService disruptorService;
    private final CanvasExecutionRequestService requestService;
    private final CanvasMqTriggerRejectedMapper rejectedMapper;
    private final CanvasMetrics metrics;
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
            recordRejected("INVALID_BODY", tag);
            storeRejected(msgId, tag, "INVALID_BODY", e.getMessage(), body);
            notificationEventService.systemAlert(
                    "MQ_TRIGGER_PARSE_FAILED",
                    "MQ 触发消息解析失败",
                    trimAlert("tag=" + tag + " msgId=" + msgId + " error=" + e.getMessage() + " body=" + body),
                    "/mq-config",
                    "MQ_TRIGGER",
                    msgId,
                    "mq:parse:" + msgId,
                    null);
            return;
        }
        try {
            validateMessage(triggerMessage);
        } catch (IllegalArgumentException e) {
            recordRejected("INVALID_MESSAGE", tag);
            storeRejected(msgId, tag, "INVALID_MESSAGE", e.getMessage(), body);
            notificationEventService.systemAlert(
                    "MQ_TRIGGER_VALIDATE_FAILED",
                    "MQ 触发消息校验失败",
                    trimAlert("tag=" + tag + " msgId=" + msgId + " error=" + e.getMessage()),
                    "/mq-config",
                    "MQ_TRIGGER",
                    msgId,
                    "mq:validate:" + msgId,
                    null);
            return;
        }

        if (!routeService.isRouteReady()) {
            throw new IllegalStateException("MQ trigger route table is not ready");
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
        }
    }

    private void recordRouteRejected(String reason, String tag) {
        try {
            metrics.recordMqRouteRejected(reason, tag);
        } catch (RuntimeException ignored) {
        }
    }

    private void storeRejected(String msgId, String tag, String reason, String errorMsg, String body) {
        try {
            CanvasMqTriggerRejectedDO rejected = new CanvasMqTriggerRejectedDO();
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

    private String trimAlert(String value) {
        if (value == null || value.length() <= ALERT_CONTENT_LIMIT) {
            return value;
        }
        return value.substring(0, ALERT_CONTENT_LIMIT);
    }
}
