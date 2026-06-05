package org.chovy.canvas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.dal.mapper.EventDefinitionMapper;
import org.chovy.canvas.domain.meta.EventDefinitionCacheService;
import org.chovy.canvas.dto.EventReportReq;
import org.chovy.canvas.service.EventDefinitionService;
import org.chovy.canvas.web.EventDefinitionController;
import org.chovy.canvas.web.EventReportAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EventDefinitionControllerTest {

    private static final String REPORT_SECRET = "0123456789abcdef0123456789abcdef";

    private EventDefinitionMapper eventMapper;
    private EventDefinitionCacheService eventDefinitionCacheService;
    private EventDefinitionService eventDefinitionService;
    private EventDefinitionController controller;

    @BeforeEach
    void setUp() {
        eventMapper = mock(EventDefinitionMapper.class);
        eventDefinitionCacheService = mock(EventDefinitionCacheService.class);
        eventDefinitionService = mock(EventDefinitionService.class);
        controller = new EventDefinitionController(
                eventMapper,
                eventDefinitionCacheService,
                eventDefinitionService,
                new ObjectMapper(),
                new EventReportAuthService(REPORT_SECRET));
    }

    @Test
    void createInvalidatesEventCodeCacheIncludingCachedNull() {
        EventDefinitionDO body = new EventDefinitionDO();
        body.setEventCode("ORDER_COMPLETE");

        controller.create(body).block();

        verify(eventMapper).insert(body);
        verify(eventDefinitionCacheService).invalidatePublishedByCode("ORDER_COMPLETE");
    }

    @Test
    void updateInvalidatesOldAndNewEventCodeCache() {
        EventDefinitionDO existing = new EventDefinitionDO();
        existing.setEventCode("OLD_CODE");
        when(eventMapper.selectById(7L)).thenReturn(existing);
        EventDefinitionDO body = new EventDefinitionDO();
        body.setEventCode("NEW_CODE");

        controller.update(7L, body).block();

        verify(eventMapper).updateById(body);
        verify(eventDefinitionCacheService).invalidatePublishedByCode("OLD_CODE");
        verify(eventDefinitionCacheService).invalidatePublishedByCode("NEW_CODE");
    }

    @Test
    void deleteInvalidatesExistingEventCodeCache() {
        EventDefinitionDO existing = new EventDefinitionDO();
        existing.setEventCode("DELETE_ME");
        when(eventMapper.selectById(8L)).thenReturn(existing);

        controller.delete(8L).block();

        verify(eventMapper).deleteById(8L);
        verify(eventDefinitionCacheService).invalidatePublishedByCode("DELETE_ME");
    }

    @Test
    void reportEventRequiresValidHmacSignatureBeforeDelegating() throws Exception {
        String body = "{\"eventCode\":\"ORDER_COMPLETE\",\"userId\":\"user-99\",\"attributes\":{\"orderId\":\"ORD-001\"}}";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = hmac(timestamp + "\n" + body);
        when(eventDefinitionService.doReportEvent(any(EventReportReq.class)))
                .thenReturn(Map.of("status", "ACCEPTED"));

        MockServerHttpRequest request = MockServerHttpRequest.post("/canvas/events/report")
                .header(EventReportAuthService.TIMESTAMP_HEADER, timestamp)
                .header(EventReportAuthService.SIGNATURE_HEADER, signature)
                .body(body);

        Map<String, Object> result = controller.reportEvent(request, Mono.just(body)).block().getData();

        assertThat(result.get("status")).isEqualTo("ACCEPTED");
        ArgumentCaptor<EventReportReq> captor = ArgumentCaptor.forClass(EventReportReq.class);
        verify(eventDefinitionService).doReportEvent(captor.capture());
        assertThat(captor.getValue().getEventCode()).isEqualTo("ORDER_COMPLETE");
        assertThat(captor.getValue().getUserId()).isEqualTo("user-99");
    }

    @Test
    void reportEventRejectsSignedInvalidBodyBeforeDelegating() throws Exception {
        String body = "{\"eventCode\":\"ORDER_COMPLETE\",\"attributes\":{\"orderId\":\"ORD-001\"}}";
        String timestamp = String.valueOf(System.currentTimeMillis());
        MockServerHttpRequest request = MockServerHttpRequest.post("/canvas/events/report")
                .header(EventReportAuthService.TIMESTAMP_HEADER, timestamp)
                .header(EventReportAuthService.SIGNATURE_HEADER, hmac(timestamp + "\n" + body))
                .body(body);

        assertThatThrownBy(() -> controller.reportEvent(request, Mono.just(body)).block())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("userId");
        verifyNoInteractions(eventDefinitionService);
    }

    private static String hmac(String canonical) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(REPORT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    }
}
