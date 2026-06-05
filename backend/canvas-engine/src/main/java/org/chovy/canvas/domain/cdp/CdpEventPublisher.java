package org.chovy.canvas.domain.cdp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CdpEventPublisher {
    private final RocketMQTemplate rocketMQTemplate;

    @Value("${canvas.cdp.event-topic:CDP_EVENT_INGESTED}")
    private String topic;

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
