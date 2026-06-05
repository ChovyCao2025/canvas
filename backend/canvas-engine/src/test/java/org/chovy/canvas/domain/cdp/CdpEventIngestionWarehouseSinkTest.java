package org.chovy.canvas.domain.cdp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.domain.meta.EventDefinitionCacheService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseEventSink;
import org.chovy.canvas.dto.cdp.TrackEventReq;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpEventIngestionWarehouseSinkTest {

    @Test
    void acceptedEventIsMirroredToWarehouseSinkAfterMysqlInsert() {
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        CdpEventIngestionService service = service(sink);

        boolean accepted = service.ingestOne(key(), event(), null);

        assertThat(accepted).isTrue();
        ArgumentCaptor<CdpEventLogDO> row = ArgumentCaptor.forClass(CdpEventLogDO.class);
        verify(sink).writeAccepted(row.capture());
        assertThat(row.getValue().getId()).isEqualTo(101L);
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getEventCode()).isEqualTo("OrderPaid");
    }

    @Test
    void warehouseSinkFailureDoesNotRejectAcceptedEvent() {
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        doThrow(new IllegalStateException("doris unavailable")).when(sink).writeAccepted(any());
        CdpEventIngestionService service = service(sink);

        boolean accepted = service.ingestOne(key(), event(), null);

        assertThat(accepted).isTrue();
    }

    private CdpEventIngestionService service(CdpWarehouseEventSink sink) {
        CdpEventLogMapper eventLogMapper = mock(CdpEventLogMapper.class);
        when(eventLogMapper.selectCount(any())).thenReturn(0L);
        when(eventLogMapper.insert(any(CdpEventLogDO.class))).thenAnswer(invocation -> {
            CdpEventLogDO row = invocation.getArgument(0);
            row.setId(101L);
            return 1;
        });
        EventDefinitionDO definition = new EventDefinitionDO();
        definition.setEventCode("OrderPaid");
        definition.setAutoDiscover(0);
        EventDefinitionCacheService definitions = mock(EventDefinitionCacheService.class);
        when(definitions.getPublishedByCode("OrderPaid")).thenReturn(definition);
        CdpUserService userService = mock(CdpUserService.class);
        EventAttributeDiscoveryService discoveryService = mock(EventAttributeDiscoveryService.class);
        CdpEventPublisher publisher = mock(CdpEventPublisher.class);
        ObjectProvider<CdpWarehouseEventSink> sinkProvider = mock(ObjectProvider.class);
        when(sinkProvider.getIfAvailable()).thenReturn(sink);
        return new CdpEventIngestionService(
                eventLogMapper,
                definitions,
                userService,
                new ObjectMapper(),
                discoveryService,
                publisher,
                sinkProvider);
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
}
