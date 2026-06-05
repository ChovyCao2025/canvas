package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseRealtimeCheckpointDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseRealtimeCheckpointMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseRealtimeRetryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseRealtimeCheckpointServiceTest {

    @Test
    void recordDeliveredUpsertsLatestRealtimeCheckpoint() {
        CdpWarehouseRealtimeCheckpointMapper checkpointMapper = mock(CdpWarehouseRealtimeCheckpointMapper.class);
        CdpWarehouseRealtimeCheckpointService service = service(checkpointMapper, mock(CdpWarehouseRealtimeRetryMapper.class));

        service.recordDelivered(event(101L), "INGESTION");

        ArgumentCaptor<CdpWarehouseRealtimeCheckpointDO> row =
                ArgumentCaptor.forClass(CdpWarehouseRealtimeCheckpointDO.class);
        verify(checkpointMapper).upsertDelivered(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getStreamKey()).isEqualTo("CDP_EVENT_ODS");
        assertThat(row.getValue().getLastEventLogId()).isEqualTo(101L);
        assertThat(row.getValue().getLastMessageId()).isEqualTo("msg-101");
        assertThat(row.getValue().getLastDeliverySource()).isEqualTo("INGESTION");
        assertThat(row.getValue().getLastDeliveredAt()).isNotNull();
    }

    @Test
    void recordFailureUpsertsFailureCheckpoint() {
        CdpWarehouseRealtimeCheckpointMapper checkpointMapper = mock(CdpWarehouseRealtimeCheckpointMapper.class);
        CdpWarehouseRealtimeCheckpointService service = service(checkpointMapper, mock(CdpWarehouseRealtimeRetryMapper.class));

        service.recordFailure(event(101L), "doris unavailable");

        ArgumentCaptor<CdpWarehouseRealtimeCheckpointDO> row =
                ArgumentCaptor.forClass(CdpWarehouseRealtimeCheckpointDO.class);
        verify(checkpointMapper).upsertFailure(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getStreamKey()).isEqualTo("CDP_EVENT_ODS");
        assertThat(row.getValue().getLastFailureAt()).isNotNull();
        assertThat(row.getValue().getLastFailureMessage()).contains("doris unavailable");
    }

    @Test
    void statusReturnsCheckpointsAndRetryBacklogCounts() {
        CdpWarehouseRealtimeCheckpointMapper checkpointMapper = mock(CdpWarehouseRealtimeCheckpointMapper.class);
        CdpWarehouseRealtimeCheckpointDO checkpoint = new CdpWarehouseRealtimeCheckpointDO();
        checkpoint.setId(1L);
        checkpoint.setTenantId(9L);
        checkpoint.setStreamKey("CDP_EVENT_ODS");
        checkpoint.setLastEventLogId(101L);
        checkpoint.setDeliveredCount(10L);
        checkpoint.setFailureCount(2L);
        when(checkpointMapper.selectList(any())).thenReturn(List.of(checkpoint));
        CdpWarehouseRealtimeRetryMapper retryMapper = mock(CdpWarehouseRealtimeRetryMapper.class);
        when(retryMapper.selectCount(any())).thenReturn(3L, 1L);
        CdpWarehouseRealtimeCheckpointService service = service(checkpointMapper, retryMapper);

        CdpWarehouseRealtimeCheckpointService.RealtimeStatus status = service.status(9L);

        assertThat(status.tenantId()).isEqualTo(9L);
        assertThat(status.checkpoints()).hasSize(1);
        assertThat(status.checkpoints().get(0).streamKey()).isEqualTo("CDP_EVENT_ODS");
        assertThat(status.liveRetryCount()).isEqualTo(3);
        assertThat(status.deadRetryCount()).isEqualTo(1);
    }

    private CdpWarehouseRealtimeCheckpointService service(CdpWarehouseRealtimeCheckpointMapper checkpointMapper,
                                                          CdpWarehouseRealtimeRetryMapper retryMapper) {
        return new CdpWarehouseRealtimeCheckpointService(checkpointMapper, retryMapper);
    }

    private CdpEventLogDO event(Long id) {
        CdpEventLogDO row = new CdpEventLogDO();
        row.setId(id);
        row.setTenantId(9L);
        row.setMessageId("msg-" + id);
        row.setEventCode("OrderPaid");
        row.setEventTime(LocalDateTime.of(2026, 6, 5, 10, 11, 12));
        row.setReceivedAt(LocalDateTime.of(2026, 6, 5, 10, 11, 13));
        return row;
    }
}
