package org.chovy.canvas.domain.cdp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.domain.meta.EventDefinitionCacheService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseEventSink;
import org.chovy.canvas.dto.cdp.BatchTrackReq;
import org.chovy.canvas.dto.cdp.IngestionResult;
import org.chovy.canvas.dto.cdp.TrackEventReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CdpEventIngestionServiceTest {
    private CdpEventLogMapper eventLogMapper;
    private EventDefinitionCacheService eventDefinitionCacheService;
    private CdpUserService userService;
    private EventAttributeDiscoveryService discoveryService;
    private CdpEventPublisher publisher;
    private CdpEventIngestionService service;

    @BeforeEach
    void setUp() {
        eventLogMapper = mock(CdpEventLogMapper.class);
        eventDefinitionCacheService = mock(EventDefinitionCacheService.class);
        userService = mock(CdpUserService.class);
        discoveryService = mock(EventAttributeDiscoveryService.class);
        publisher = mock(CdpEventPublisher.class);
        service = new CdpEventIngestionService(
                eventLogMapper,
                eventDefinitionCacheService,
                userService,
                new ObjectMapper(),
                discoveryService,
                publisher,
                provider(null));
        when(eventLogMapper.selectCount(any())).thenReturn(0L);
        when(eventDefinitionCacheService.getPublishedByCode("OrderComplete"))
                .thenReturn(definition("OrderComplete"));
    }

    @Test
    void ingestStoresBatchEventsWithSdkContext() {
        TrackEventReq event = new TrackEventReq(
                "msg-1",
                "track",
                "OrderComplete",
                "user-1",
                "anon-1",
                "idem-1",
                Map.of("amount", 99.9, "currency", "CNY"),
                Map.of(
                        "session", Map.of("sessionId", "sess-1"),
                        "device", Map.of("id", "dev-1"),
                        "library", Map.of("name", "@canvas/analytics-web")),
                OffsetDateTime.parse("2026-05-30T10:00:00Z"),
                OffsetDateTime.parse("2026-05-30T10:00:01Z"));

        IngestionResult result = service.ingestBatch(key(), new BatchTrackReq(List.of(event), event.sentAt()));

        ArgumentCaptor<CdpEventLogDO> captor = ArgumentCaptor.forClass(CdpEventLogDO.class);
        verify(eventLogMapper).insert(captor.capture());
        assertThat(result.accepted()).isEqualTo(1);
        assertThat(captor.getValue().getTenantId()).isEqualTo(42L);
        assertThat(captor.getValue().getWriteKeyId()).isEqualTo(7L);
        assertThat(captor.getValue().getMessageId()).isEqualTo("msg-1");
        assertThat(captor.getValue().getEventCode()).isEqualTo("OrderComplete");
        assertThat(captor.getValue().getSessionId()).isEqualTo("sess-1");
        assertThat(captor.getValue().getDeviceId()).isEqualTo("dev-1");
        assertThat(captor.getValue().getProperties()).contains("amount");
        assertThat(captor.getValue().getSdkContext()).contains("@canvas/analytics-web");
        verify(userService).ensureUser(42L, "user-1", "CDP_EVENT", "OrderComplete");
    }

    @Test
    void ingestSkipsDuplicateMessageIdWithinTenant() {
        when(eventLogMapper.selectCount(any())).thenReturn(1L);

        IngestionResult result = service.ingestBatch(
                key(),
                new BatchTrackReq(List.of(validEvent("msg-1")), OffsetDateTime.now()));

        assertThat(result.accepted()).isZero();
        verify(eventLogMapper, never()).insert(org.mockito.ArgumentMatchers.<CdpEventLogDO>any());
    }

    @Test
    void ingestRejectsUnknownTrackEvent() {
        when(eventDefinitionCacheService.getPublishedByCode("Unknown")).thenReturn(null);

        IngestionResult result = service.ingestBatch(
                key(),
                new BatchTrackReq(List.of(validEvent("msg-1", "Unknown")), OffsetDateTime.now()));

        assertThat(result.rejected()).isEqualTo(1);
        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.messageId()).isEqualTo("msg-1");
            assertThat(error.code()).isEqualTo("INVALID_EVENT");
            assertThat(error.message()).contains("unknown event code");
        });
        verify(eventLogMapper, never()).insert(org.mockito.ArgumentMatchers.<CdpEventLogDO>any());
    }

    @Test
    void acceptedAutoDiscoverableEventDiscoversAttributesAndPublishesInternalEvent() {
        EventDefinitionDO def = definition("OrderComplete");
        def.setAutoDiscover(1);
        when(eventDefinitionCacheService.getPublishedByCode("OrderComplete")).thenReturn(def);

        service.ingestBatch(key(), new BatchTrackReq(List.of(validEvent("msg-1")), OffsetDateTime.now()));

        verify(discoveryService).discover(
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq("OrderComplete"),
                org.mockito.ArgumentMatchers.anyMap());
        verify(publisher).publishAccepted(org.mockito.ArgumentMatchers.any(CdpEventLogDO.class));
    }

    @Test
    void duplicateEventDoesNotPublishInternalEvent() {
        when(eventLogMapper.selectCount(any())).thenReturn(1L);

        service.ingestBatch(key(), new BatchTrackReq(List.of(validEvent("msg-1")), OffsetDateTime.now()));

        verifyNoInteractions(discoveryService);
        verifyNoInteractions(publisher);
    }

    private CdpWriteKeyAuthService.AuthenticatedWriteKey key() {
        return new CdpWriteKeyAuthService.AuthenticatedWriteKey(7L, 42L, "ck_test_abc", "WEB", 100, null);
    }

    private TrackEventReq validEvent(String messageId) {
        return validEvent(messageId, "OrderComplete");
    }

    private TrackEventReq validEvent(String messageId, String eventCode) {
        return new TrackEventReq(
                messageId,
                "track",
                eventCode,
                "user-1",
                "anon-1",
                "idem-" + messageId,
                Map.of("amount", 20),
                Map.of("sessionId", "sess-1", "deviceId", "dev-1", "platform", "WEB"),
                OffsetDateTime.parse("2026-05-30T10:00:00Z"),
                null);
    }

    private EventDefinitionDO definition(String eventCode) {
        EventDefinitionDO definition = new EventDefinitionDO();
        definition.setEventCode(eventCode);
        definition.setAutoDiscover(0);
        return definition;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<CdpWarehouseEventSink> provider(CdpWarehouseEventSink value) {
        ObjectProvider<CdpWarehouseEventSink> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
