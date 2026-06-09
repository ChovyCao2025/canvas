package org.chovy.canvas.domain.cdp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CdpEventPublisher 编排 domain.cdp 场景的领域业务规则。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdpEventPublisher {
    private final RocketMQTemplate rocketMQTemplate;

    @Value("${canvas.cdp.event-topic:CDP_EVENT_INGESTED}")
    private String topic;

    /**
     * 发布已接收的 CDP 事件到 RocketMQ。
     *
     * <p>方法把事件日志中的租户、事件 ID、消息 ID、用户标识、事件时间和属性组装为消息体；
     * 如果 eventCode 非空，会使用 {@code topic:eventCode} 作为目的地以便消费者按事件过滤。发送为同步调用，
     * RocketMQ 失败会向调用方抛出异常。</p>
     *
     * @param event 已持久化且通过接收校验的 CDP 事件日志
     */
    public void publishAccepted(CdpEventLogDO event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", event.getTenantId());
        payload.put("eventLogId", event.getId());
        payload.put("messageId", event.getMessageId());
        payload.put("eventCode", event.getEventCode());
        payload.put("userId", event.getUserId());
        payload.put("anonymousId", event.getAnonymousId());
        payload.put("eventTime", event.getEventTime() == null ? null : event.getEventTime().toString());
        payload.put("properties", event.getProperties());
        String destination = event.getEventCode() == null || event.getEventCode().isBlank()
                ? topic
                : topic + ":" + event.getEventCode();
        rocketMQTemplate.syncSend(destination, payload);
    }
}
