package org.chovy.canvas.domain.cdp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.domain.meta.EventDefinitionCacheService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseEventSink;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyTombstoneService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeCheckpointService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeRetryService;
import org.chovy.canvas.dto.cdp.BatchTrackReq;
import org.chovy.canvas.dto.cdp.IngestionResult;
import org.chovy.canvas.dto.cdp.TrackEventReq;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CdpEventIngestionPrivacyTombstoneGuardTest {

    @Test
    void tombstonedEventIsRejectedBeforeAnyCdpOrWarehouseSideEffects() {
        CdpEventLogMapper eventLogMapper = mock(CdpEventLogMapper.class);
        when(eventLogMapper.selectCount(any())).thenReturn(0L);
        EventDefinitionDO definition = new EventDefinitionDO();
        definition.setEventCode("OrderPaid");
        definition.setAutoDiscover(1);
        EventDefinitionCacheService definitions = mock(EventDefinitionCacheService.class);
        when(definitions.getPublishedByCode("OrderPaid")).thenReturn(definition);
        CdpUserService userService = mock(CdpUserService.class);
        EventAttributeDiscoveryService discoveryService = mock(EventAttributeDiscoveryService.class);
        CdpEventPublisher publisher = mock(CdpEventPublisher.class);
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        CdpWarehouseRealtimeRetryService retryService = mock(CdpWarehouseRealtimeRetryService.class);
        CdpWarehouseRealtimeCheckpointService checkpointService =
                mock(CdpWarehouseRealtimeCheckpointService.class);
        CdpWarehousePrivacyTombstoneService tombstoneService =
                mock(CdpWarehousePrivacyTombstoneService.class);
        doThrow(new CdpWarehousePrivacyTombstoneService.PrivacyTombstoneViolationException("blocked user"))
                .when(tombstoneService)
                .enforceNotBlocked(9L, "USER_ID", "user-1", "CDP_EVENT_INGESTION");
        ObjectProvider<CdpWarehouseEventSink> sinkProvider = provider(sink);
        ObjectProvider<CdpWarehouseRealtimeRetryService> retryProvider = provider(retryService);
        ObjectProvider<CdpWarehouseRealtimeCheckpointService> checkpointProvider = provider(checkpointService);
        ObjectProvider<CdpWarehousePrivacyTombstoneService> tombstoneProvider = provider(tombstoneService);
        CdpEventIngestionService service = new CdpEventIngestionService(
                eventLogMapper,
                definitions,
                userService,
                new ObjectMapper(),
                discoveryService,
                publisher,
                sinkProvider,
                retryProvider,
                checkpointProvider,
                tombstoneProvider);

        IngestionResult result = service.ingestBatch(key(), new BatchTrackReq(
                java.util.List.of(event()), OffsetDateTime.parse("2026-06-05T10:11:13Z")));

        assertThat(result.accepted()).isZero();
        assertThat(result.rejected()).isEqualTo(1);
        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.messageId()).isEqualTo("msg-1");
            assertThat(error.code()).isEqualTo("INVALID_EVENT");
            assertThat(error.message()).contains("blocked user");
        });
        verify(tombstoneService).enforceNotBlocked(9L, "USER_ID", "user-1", "CDP_EVENT_INGESTION");
        verify(eventLogMapper, never()).insert(any(CdpEventLogDO.class));
        verifyNoInteractions(userService, discoveryService, publisher, sink, retryService, checkpointService);
        verify(sinkProvider, never()).getIfAvailable();
        verify(retryProvider, never()).getIfAvailable();
        verify(checkpointProvider, never()).getIfAvailable();
    }

    private CdpWriteKeyAuthService.AuthenticatedWriteKey key() {
        return new CdpWriteKeyAuthService.AuthenticatedWriteKey(1L, 9L, "ck_prefix", "web", 100, null);
    }

    private TrackEventReq event() {
        return new TrackEventReq(
                "msg-1",
                "track",
                "OrderPaid",
                "user-1",
                "anon-1",
                "idem-1",
                Map.of("amount", 20),
                Map.of("sessionId", "session-1", "deviceId", "device-1", "platform", "web"),
                OffsetDateTime.parse("2026-06-05T10:11:12Z"),
                null);
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
