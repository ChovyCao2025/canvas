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
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
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

    /** 溢出重试写入 DLQ 时使用的失败节点标识。 */
    private static final String DLQ_FAILED_NODE_ID = "OVERFLOW_RETRY";

    /** Disruptor 投递服务，用于重新发布溢出重试请求。 */
    private final CanvasDisruptorService disruptor;
    /** 触发优先级配置，提供溢出最大重试次数。 */
    private final TriggerPriorityConfig priorityConfig;
    /** 溢出重试消息反序列化组件。 */
    private final ObjectMapper objectMapper;
    /** 执行死信表 Mapper，用于记录超过重试上限的请求。 */
    private final CanvasExecutionDlqMapper dlqMapper;

    /**
     * 消费或监听 on Message 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param message message 方法执行所需的业务参数
     */
    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        OverflowRetryMessage msg;
        try {
            // 溢出重试消息格式错误时抛异常，让 RocketMQ 按消费失败处理，而不是静默丢弃。
            msg = objectMapper.readValue(body, OverflowRetryMessage.class);
        } catch (Exception e) {
            log.error("[OVERFLOW_RETRY] 消息解析失败 msgId={}: {}", message.getMsgId(), e.getMessage());
            throw new IllegalArgumentException("溢出重试消息体格式错误: " + e.getMessage(), e);
        }

        // 总重试次数 = 链路累计次数 + RocketMQ 当前消息重投次数，避免跨延迟消息时计数归零。
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

        // 重投回 Disruptor 时携带累计 retryCount，由执行入口决定是否继续排队或再次溢出。
        disruptor.publishOverflowRetry(
                msg.getCanvasId(), msg.getUserId(), msg.getTriggerType(),
                msg.getTriggerNodeType(), msg.getMatchKey(),
                payload, msg.getMsgId(), totalRetry
        );
    }

        /**
     * 执行 copy Payload 对应的业务逻辑。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param payload payload 请求体、消息体或事件载荷
     * @return 按业务键组织的映射结果
     */
    private Map<String, Object> copyPayload(Map<String, Object> payload) {
        Map<String, Object> copy = new HashMap<>();
        if (payload != null) {
            copy.putAll(payload);
        }
        // 旧版本曾把重试控制字段塞进 payload；这里移除，避免业务节点读到内部控制键。
        copy.remove(OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY);
        return copy;
    }

    /**
     * 写入或记录 write Dlq 相关的业务数据。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param msg msg 方法执行所需的业务参数
     * @param totalRetry totalRetry 方法执行所需的业务参数
     * @param rocketMqMsgId rocketMqMsgId 对应的业务主键或标识
     */
    private void writeDlq(OverflowRetryMessage msg, int totalRetry, String rocketMqMsgId) {
        try {
            // 达到最大重试后转 DLQ，保留触发上下文，便于后台重放或人工定位溢出热点。
            CanvasExecutionDlqDO dlq = CanvasExecutionDlqDO.builder()
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

    /**
     * 执行 non Blank 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
