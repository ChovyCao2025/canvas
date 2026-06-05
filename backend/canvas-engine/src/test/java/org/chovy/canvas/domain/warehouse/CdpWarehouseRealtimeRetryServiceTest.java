package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseRealtimeRetryDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseRealtimeRetryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseRealtimeRetryServiceTest {

    @Test
    void enqueueFailureUpsertsOneRetryRowPerEventLog() {
        CdpWarehouseRealtimeRetryMapper retryMapper = mock(CdpWarehouseRealtimeRetryMapper.class);
        CdpWarehouseRealtimeRetryService service = service(retryMapper, mock(CdpEventLogMapper.class), mock(CdpWarehouseEventSink.class));

        service.enqueueFailure(event(101L), "doris unavailable");

        ArgumentCaptor<CdpWarehouseRealtimeRetryDO> row =
                ArgumentCaptor.forClass(CdpWarehouseRealtimeRetryDO.class);
        verify(retryMapper).upsertPending(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getEventLogId()).isEqualTo(101L);
        assertThat(row.getValue().getMessageId()).isEqualTo("msg-101");
        assertThat(row.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(row.getValue().getLastError()).contains("doris unavailable");
    }

    @Test
    void retryDueMarksSuccessWhenSinkAcceptsEvent() {
        CdpWarehouseRealtimeRetryMapper retryMapper = retryMapper(List.of(retry(1L, 101L, 0)));
        CdpEventLogMapper eventLogMapper = mock(CdpEventLogMapper.class);
        CdpEventLogDO event = event(101L);
        when(eventLogMapper.selectById(101L)).thenReturn(event);
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        CdpWarehouseRealtimeCheckpointService checkpointService = mock(CdpWarehouseRealtimeCheckpointService.class);
        CdpWarehouseRealtimeRetryService service = service(retryMapper, eventLogMapper, sink, checkpointService);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);

        CdpWarehouseRealtimeRetryService.RetryResult result = service.retryDue(now, 10, 3);

        assertThat(result.success()).isEqualTo(1);
        verify(sink).writeAccepted(event);
        verify(retryMapper).markSuccess(1L, now);
        verify(checkpointService).recordDelivered(event, "RETRY");
    }

    @Test
    void retryDueSchedulesRetryWhenSinkFailsBeforeMaxAttempts() {
        CdpWarehouseRealtimeRetryMapper retryMapper = retryMapper(List.of(retry(1L, 101L, 0)));
        CdpEventLogMapper eventLogMapper = mock(CdpEventLogMapper.class);
        when(eventLogMapper.selectById(101L)).thenReturn(event(101L));
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        doThrow(new IllegalStateException("doris unavailable")).when(sink).writeAccepted(any());
        CdpWarehouseRealtimeCheckpointService checkpointService = mock(CdpWarehouseRealtimeCheckpointService.class);
        CdpWarehouseRealtimeRetryService service = service(retryMapper, eventLogMapper, sink, checkpointService);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);

        CdpWarehouseRealtimeRetryService.RetryResult result = service.retryDue(now, 10, 3);

        assertThat(result.retried()).isEqualTo(1);
        verify(retryMapper).markRetry(eq(1L), contains("doris unavailable"), any(LocalDateTime.class), eq(now));
        verify(checkpointService).recordFailure(any(CdpEventLogDO.class), contains("doris unavailable"));
    }

    @Test
    void retryDueMarksDeadWhenMaxAttemptsReached() {
        CdpWarehouseRealtimeRetryMapper retryMapper = retryMapper(List.of(retry(1L, 101L, 2)));
        CdpEventLogMapper eventLogMapper = mock(CdpEventLogMapper.class);
        when(eventLogMapper.selectById(101L)).thenReturn(event(101L));
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        doThrow(new IllegalStateException("doris unavailable")).when(sink).writeAccepted(any());
        CdpWarehouseRealtimeRetryService service = service(retryMapper, eventLogMapper, sink);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);

        CdpWarehouseRealtimeRetryService.RetryResult result = service.retryDue(now, 10, 3);

        assertThat(result.dead()).isEqualTo(1);
        verify(retryMapper).markDead(1L, "doris unavailable", now);
    }

    private CdpWarehouseRealtimeRetryMapper retryMapper(List<CdpWarehouseRealtimeRetryDO> rows) {
        CdpWarehouseRealtimeRetryMapper retryMapper = mock(CdpWarehouseRealtimeRetryMapper.class);
        when(retryMapper.selectList(any())).thenReturn(rows);
        when(retryMapper.claimDue(any(), any(), any())).thenReturn(1);
        return retryMapper;
    }

    private CdpWarehouseRealtimeRetryService service(CdpWarehouseRealtimeRetryMapper retryMapper,
                                                     CdpEventLogMapper eventLogMapper,
                                                     CdpWarehouseEventSink sink) {
        return new CdpWarehouseRealtimeRetryService(retryMapper, eventLogMapper, sink);
    }

    private CdpWarehouseRealtimeRetryService service(CdpWarehouseRealtimeRetryMapper retryMapper,
                                                     CdpEventLogMapper eventLogMapper,
                                                     CdpWarehouseEventSink sink,
                                                     CdpWarehouseRealtimeCheckpointService checkpointService) {
        return new CdpWarehouseRealtimeRetryService(retryMapper, eventLogMapper, sink, checkpointService);
    }

    private CdpWarehouseRealtimeRetryDO retry(Long id, Long eventLogId, int attemptCount) {
        CdpWarehouseRealtimeRetryDO retry = new CdpWarehouseRealtimeRetryDO();
        retry.setId(id);
        retry.setTenantId(9L);
        retry.setEventLogId(eventLogId);
        retry.setStatus("RETRY");
        retry.setAttemptCount(attemptCount);
        return retry;
    }

    private CdpEventLogDO event(Long id) {
        CdpEventLogDO row = new CdpEventLogDO();
        row.setId(id);
        row.setTenantId(9L);
        row.setMessageId("msg-" + id);
        row.setEventCode("OrderPaid");
        row.setStatus(CdpEventLogDO.ACCEPTED);
        return row;
    }
}
