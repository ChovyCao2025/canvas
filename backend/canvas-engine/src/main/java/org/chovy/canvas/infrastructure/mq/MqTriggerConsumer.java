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

    /** 系统告警内容最大长度，避免异常消息体过长。 */
    private static final int ALERT_CONTENT_LIMIT = 900;

    /** MQ 触发消息反序列化组件。 */
    private final ObjectMapper objectMapper;
    /** 触发路由服务，用于按 topic 查询命中画布。 */
    private final TriggerRouteService routeService;
    /** Disruptor 投递服务，用于进入内部执行队列。 */
    private final CanvasDisruptorService disruptorService;
    /** 执行请求服务，用于生成画布执行请求。 */
    private final CanvasExecutionRequestService requestService;
    /** MQ 触发拒绝记录 Mapper。 */
    private final CanvasMqTriggerRejectedMapper rejectedMapper;
    /** MQ 触发链路指标采集器。 */
    private final CanvasMetrics metrics;
    /** 通知事件服务，用于发送 MQ 触发异常告警。 */
    private final NotificationEventService notificationEventService;

    /**
     * 消费或监听 on Message 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param message message 方法执行所需的业务参数
     */
    @Override
    public void onMessage(MessageExt message) {
        String tag = message.getTags();
        String msgId = message.getMsgId();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        log.info("[MQ_CONSUMER] 收到消息 tag={} msgId={}", tag, msgId);

        MqTriggerMessage triggerMessage;
        try {
            // 解析失败属于不可执行消息：记录拒绝原因和告警后 ACK，避免毒消息无限重试。
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
            // 结构合法但业务字段缺失，同样落 rejected 表供后续排查或人工补偿。
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
            // 路由重建窗口内不消费消息，抛异常交给 RocketMQ 重投，避免按空路由误丢弃。
            throw new IllegalStateException("MQ trigger route table is not ready");
        }

        Set<String> canvasIds = routeService.getCanvasByMqTopic(tag);
        if (canvasIds.isEmpty()) {
            // 路由就绪但无匹配画布说明配置缺失或 tag 未订阅，按业务丢弃并发出告警。
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
            // 每个命中的画布生成独立执行请求，共用 RocketMQ msgId 做后续幂等和链路追踪。
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

        /**
     * 构建、解析或转换 parse Canvas Id 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param canvasIdStr canvasIdStr 画布相关对象或标识
     * @param tag tag 方法执行所需的业务参数
     * @return 计算得到的数值结果
     */
    private Long parseCanvasId(String canvasIdStr, String tag) {
        try {
            long canvasId = Long.parseLong(canvasIdStr);
            if (canvasId <= 0) {
                throw new NumberFormatException("non-positive canvasId");
            }
            return canvasId;
        } catch (RuntimeException e) {
            // 非法路由成员只影响当前画布 ID，不阻断同 topic 下其他画布继续触发。
            log.warn("[MQ_CONSUMER] 跳过非法路由 canvasId={} tag={}", canvasIdStr, tag);
            recordRouteRejected("INVALID_CANVAS_ID", tag);
            return null;
        }
    }

        /**
     * 校验 validate Message 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param message message 方法执行所需的业务参数
     */
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

        /**
     * 写入或记录 record Rejected 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param reason reason 方法执行所需的业务参数
     * @param tag tag 方法执行所需的业务参数
     */
    private void recordRejected(String reason, String tag) {
        try {
            metrics.recordMqTriggerRejected(reason, tag);
        } catch (RuntimeException ignored) {
        }
    }

        /**
     * 写入或记录 record Route Rejected 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param reason reason 方法执行所需的业务参数
     * @param tag tag 方法执行所需的业务参数
     */
    private void recordRouteRejected(String reason, String tag) {
        try {
            metrics.recordMqRouteRejected(reason, tag);
        } catch (RuntimeException ignored) {
        }
    }

    /**
     * 执行 store Rejected 对应的业务逻辑。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param msgId msgId 对应的业务主键或标识
     * @param tag tag 方法执行所需的业务参数
     * @param reason reason 方法执行所需的业务参数
     * @param errorMsg errorMsg 方法执行所需的业务参数
     * @param body body 请求体、消息体或事件载荷
     */
    private void storeRejected(String msgId, String tag, String reason, String errorMsg, String body) {
        try {
            // rejected 表保存原始消息和截断后的错误信息，作为 MQ 消费降级后的可观测出口。
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

        /**
     * 执行 trim 对应的业务逻辑。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param maxLength maxLength 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

        /**
     * 执行 trim Alert 对应的业务逻辑。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 转换或查询得到的字符串结果
     */
    private String trimAlert(String value) {
        if (value == null || value.length() <= ALERT_CONTENT_LIMIT) {
            return value;
        }
        return value.substring(0, ALERT_CONTENT_LIMIT);
    }
}
