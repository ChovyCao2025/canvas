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
import org.chovy.canvas.domain.execution.CanvasExecutionDlq;
import org.chovy.canvas.domain.execution.CanvasExecutionDlqMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.trigger.TriggerPriorityConfig;
import org.chovy.canvas.perf.PerfRunContext;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 并发溢出重试消费者。
 * 读取延迟投递的溢出消息，重新发布到 Disruptor。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "CANVAS_TRIGGER_OVERFLOW",
        consumerGroup = "GID_CANVAS_OVERFLOW_RETRY",
        selectorType = SelectorType.TAG,
        selectorExpression = "*",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadNumber = 5
)
public class OverflowRetryConsumer implements RocketMQListener<MessageExt> {

    private static final String DLQ_FAILED_NODE_ID = "OVERFLOW_RETRY";

    private final CanvasDisruptorService disruptor;
    private final TriggerPriorityConfig priorityConfig;
    private final ObjectMapper objectMapper;
    private final CanvasExecutionDlqMapper dlqMapper;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        OverflowRetryMessage msg;
        try {
            msg = objectMapper.readValue(body, OverflowRetryMessage.class);
        } catch (Exception e) {
            log.error("[OVERFLOW_RETRY] 消息解析失败 msgId={}: {}", message.getMsgId(), e.getMessage());
            throw new IllegalArgumentException("溢出重试消息体格式错误: " + e.getMessage(), e);
        }

        int totalRetry = msg.getChainRetryCount() + message.getReconsumeTimes();
        if (totalRetry >= priorityConfig.getOverflowMaxRetry()) {
            log.warn("[OVERFLOW_RETRY] 超过最大重试次数 canvasId={} userId={} retryCount={}, 写入DLQ",
                    msg.getCanvasId(), msg.getUserId(), totalRetry);
            writeDlq(msg, totalRetry, message.getMsgId());
            return;
        }

        Map<String, Object> payload = copyPayload(msg.getPayload());
        log.info("[OVERFLOW_RETRY] 重试投递 canvasId={} userId={} retryCount={}",
                msg.getCanvasId(), msg.getUserId(), totalRetry);

        disruptor.publishOverflowRetry(
                msg.getCanvasId(), msg.getUserId(), msg.getTriggerType(),
                msg.getTriggerNodeType(), msg.getMatchKey(),
                payload, msg.getMsgId(), totalRetry
        );
    }

    private Map<String, Object> copyPayload(Map<String, Object> payload) {
        Map<String, Object> copy = new HashMap<>();
        if (payload != null) {
            copy.putAll(payload);
        }
        copy.remove(OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY);
        return copy;
    }

    private void writeDlq(OverflowRetryMessage msg, int totalRetry, String rocketMqMsgId) {
        try {
            CanvasExecutionDlq dlq = CanvasExecutionDlq.builder()
                    .executionId(nonBlank(msg.getMsgId()) ? msg.getMsgId() : rocketMqMsgId)
                    .canvasId(msg.getCanvasId())
                    .userId(msg.getUserId())
                    .perfRunId(PerfRunContext.extract(msg.getPayload()))
                    .failedNodeId(DLQ_FAILED_NODE_ID)
                    .failedNodeType(msg.getTriggerNodeType())
                    .errorMsg("overflow_max_retry")
                    .retryCount(totalRetry)
                    .triggerPayload(objectMapper.writeValueAsString(msg.getPayload()))
                    .triggerType(msg.getTriggerType())
                    .triggerNodeType(msg.getTriggerNodeType())
                    .matchKey(msg.getMatchKey())
                    .failedAt(LocalDateTime.now())
                    .build();
            dlqMapper.insert(dlq);
        } catch (Exception e) {
            log.error("[OVERFLOW_RETRY] 写入DLQ失败 canvasId={} msgId={}: {}",
                    msg.getCanvasId(), msg.getMsgId(), e.getMessage());
            throw new IllegalStateException("溢出重试DLQ写入失败", e);
        }
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
