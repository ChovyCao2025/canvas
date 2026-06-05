package org.chovy.canvas.domain.cdp;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CdpEventPublisherTest {

    @Test
    void publishAcceptedSendsCompactInternalEventByEventCode() {
        RocketMQTemplate rocketMQTemplate = mock(RocketMQTemplate.class);
        CdpEventPublisher publisher = new CdpEventPublisher(rocketMQTemplate);
        ReflectionTestUtils.setField(publisher, "topic", "CDP_EVENT_INGESTED");

        publisher.publishAccepted(row());

        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(rocketMQTemplate).syncSend(eq("CDP_EVENT_INGESTED:OrderComplete"), payload.capture());
        assertThat(payload.getValue())
                .containsEntry("tenantId", 42L)
                .containsEntry("eventLogId", 99L)
                .containsEntry("messageId", "msg-1")
                .containsEntry("eventCode", "OrderComplete")
                .containsEntry("userId", "user-1")
                .containsEntry("anonymousId", "anon-1")
                .containsEntry("properties", "{\"amount\":99.9}");
        assertThat(payload.getValue().get("eventTime")).isEqualTo("2026-05-30T10:00");
    }

    private CdpEventLogDO row() {
        CdpEventLogDO row = new CdpEventLogDO();
        row.setId(99L);
        row.setTenantId(42L);
        row.setMessageId("msg-1");
        row.setEventCode("OrderComplete");
        row.setUserId("user-1");
        row.setAnonymousId("anon-1");
        row.setEventTime(LocalDateTime.parse("2026-05-30T10:00:00"));
        row.setProperties("{\"amount\":99.9}");
        return row;
    }
}
