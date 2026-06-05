package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseSyncRunDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseSyncRunMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseWatermarkMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseBackfillServiceTest {

    @Test
    void backfillRejectsNonPositiveLimit() {
        CdpWarehouseBackfillService service = service(mock(CdpWarehouseEventSink.class), List.of());

        assertThatThrownBy(() -> service.backfill(9L, 0L, 0, "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit must be positive");
    }

    @Test
    void backfillLoadsAcceptedRowsAfterLastIdAndRecordsSuccess() {
        CdpEventLogDO row1 = eventRow(11L);
        CdpEventLogDO row2 = eventRow(12L);
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        CdpWarehouseBackfillService service = service(sink, List.of(row1, row2));

        CdpWarehouseBackfillService.BackfillResult result = service.backfill(9L, 10L, 100, "operator");

        assertThat(result.loaded()).isEqualTo(2);
        assertThat(result.failed()).isZero();
        verify(sink).writeAccepted(row1);
        verify(sink).writeAccepted(row2);
    }

    @Test
    void backfillRecordsFailedRunWhenSinkThrows() {
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        doThrow(new IllegalStateException("doris unavailable")).when(sink).writeAccepted(any());
        CdpWarehouseSyncRunMapper runMapper = mock(CdpWarehouseSyncRunMapper.class);
        CdpWarehouseBackfillService service = service(sink, List.of(eventRow(11L)), runMapper);

        CdpWarehouseBackfillService.BackfillResult result = service.backfill(9L, 10L, 100, "operator");

        assertThat(result.loaded()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        ArgumentCaptor<CdpWarehouseSyncRunDO> run = ArgumentCaptor.forClass(CdpWarehouseSyncRunDO.class);
        verify(runMapper).updateById(run.capture());
        assertThat(run.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(run.getValue().getErrorMessage()).contains("doris unavailable");
    }

    private CdpWarehouseBackfillService service(CdpWarehouseEventSink sink, List<CdpEventLogDO> rows) {
        return service(sink, rows, mock(CdpWarehouseSyncRunMapper.class));
    }

    private CdpWarehouseBackfillService service(CdpWarehouseEventSink sink,
                                                List<CdpEventLogDO> rows,
                                                CdpWarehouseSyncRunMapper runMapper) {
        CdpEventLogMapper eventLogMapper = mock(CdpEventLogMapper.class);
        when(eventLogMapper.selectList(any())).thenReturn(rows);
        return new CdpWarehouseBackfillService(
                eventLogMapper,
                sink,
                runMapper,
                mock(CdpWarehouseWatermarkMapper.class));
    }

    private CdpEventLogDO eventRow(Long id) {
        CdpEventLogDO row = new CdpEventLogDO();
        row.setId(id);
        row.setTenantId(9L);
        row.setStatus(CdpEventLogDO.ACCEPTED);
        return row;
    }
}
