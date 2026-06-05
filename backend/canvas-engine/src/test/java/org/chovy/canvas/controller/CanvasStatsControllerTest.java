package org.chovy.canvas.web;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.CanvasExecutionStatsDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.chovy.canvas.infrastructure.doris.DailyStatsDTO;
import org.chovy.canvas.infrastructure.doris.DorisQueryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasStatsControllerTest {

    @Test
    void getTraceReturnsEmptyWhenExecutionDoesNotBelongToCanvas() {
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        CanvasExecutionTraceMapper traceMapper = mock(CanvasExecutionTraceMapper.class);
        CanvasStatsController controller = new CanvasStatsController(executionMapper, traceMapper);
        CanvasExecutionDO execution = new CanvasExecutionDO();
        execution.setId("exec-1");
        execution.setCanvasId(20L);
        when(executionMapper.selectById("exec-1")).thenReturn(execution);

        var response = controller.getTrace(10L, "exec-1").block();

        assertThat(response.getData()).isEmpty();
        verify(traceMapper, never()).selectList(any());
    }

    @Test
    void getTracePrefersDorisRowsBeforeMysqlTraceMapper() {
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        CanvasExecutionTraceMapper traceMapper = mock(CanvasExecutionTraceMapper.class);
        DorisQueryService dorisQueryService = mock(DorisQueryService.class);
        CanvasStatsController controller = new CanvasStatsController(executionMapper, traceMapper, null, dorisQueryService);
        CanvasExecutionDO execution = new CanvasExecutionDO();
        execution.setId("exec-2");
        execution.setCanvasId(10L);
        when(executionMapper.selectById("exec-2")).thenReturn(execution);
        when(dorisQueryService.getExecutionTrace("exec-2")).thenReturn(List.of(
                new DorisQueryService.TraceRowDTO(
                        1L,
                        1L,
                        "exec-2",
                        "node-1",
                        "API_CALL",
                        "Call API",
                        1,
                        null,
                        "{\"ok\":true}",
                        null,
                        LocalDateTime.of(2026, 1, 1, 10, 0),
                        LocalDateTime.of(2026, 1, 1, 10, 0, 2),
                        null
                )));

        var response = controller.getTrace(10L, "exec-2").block();

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst())
                .containsEntry(MapFieldKeys.NODE_ID, "node-1")
                .containsEntry(MapFieldKeys.NODE_TYPE, "API_CALL")
                .containsEntry(MapFieldKeys.DURATION_MS, 2000L);
        verify(traceMapper, never()).selectList(any());
    }

    @Test
    void statsPrefersDorisDailyStatsWhenAvailable() {
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        CanvasExecutionTraceMapper traceMapper = mock(CanvasExecutionTraceMapper.class);
        CanvasExecutionStatsMapper statsMapper = mock(CanvasExecutionStatsMapper.class);
        DorisQueryService dorisQueryService = mock(DorisQueryService.class);
        CanvasStatsController controller = new CanvasStatsController(executionMapper, traceMapper, statsMapper, dorisQueryService);
        LocalDate since = LocalDate.of(2026, 1, 1);
        LocalDate until = LocalDate.of(2026, 1, 2);
        when(dorisQueryService.getDailyStats(10L, since, until)).thenReturn(List.of(
                new DailyStatsDTO(since, 10L, "Canvas", "DIRECT_CALL", 10L, 8L, 1L, 1L, 3L, 100L),
                new DailyStatsDTO(until, 10L, "Canvas", "DIRECT_CALL", 5L, 4L, 1L, 0L, 2L, 120L)
        ));

        var response = controller.stats(10L, 7, "2026-01-01", "2026-01-02").block();

        assertThat(response.getData())
                .containsEntry(MapFieldKeys.TOTAL, 15L)
                .containsEntry(MapFieldKeys.SUCCESS, 12L)
                .containsEntry(MapFieldKeys.FAILED, 2L)
                .containsEntry(MapFieldKeys.PAUSED, 1L)
                .containsEntry(MapFieldKeys.UNIQUE_USERS, 5L)
                .containsEntry(MapFieldKeys.SUCCESS_RATE, "80.0%");
        verify(statsMapper, never()).selectByCanvasIdAndDateRange(any(), any(), any());
        verify(executionMapper, never()).selectList(any());
    }

    @Test
    void statsUsesAggregateTableBeforeExecutionScan() {
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        CanvasExecutionTraceMapper traceMapper = mock(CanvasExecutionTraceMapper.class);
        CanvasExecutionStatsMapper statsMapper = mock(CanvasExecutionStatsMapper.class);
        DorisQueryService dorisQueryService = mock(DorisQueryService.class);
        CanvasStatsController controller = new CanvasStatsController(executionMapper, traceMapper, statsMapper, dorisQueryService);
        LocalDate since = LocalDate.of(2026, 1, 1);
        LocalDate until = LocalDate.of(2026, 1, 2);
        when(dorisQueryService.getDailyStats(10L, since, until)).thenReturn(List.of());
        when(statsMapper.selectByCanvasIdAndDateRange(eq(10L), eq(since), eq(until))).thenReturn(List.of(
                statsRow(since, 10, 7, 2, 1, 4),
                statsRow(until, 5, 5, 0, 0, 2)
        ));

        var response = controller.stats(10L, 7, "2026-01-01", "2026-01-02").block();

        assertThat(response.getData())
                .containsEntry(MapFieldKeys.TOTAL, 15L)
                .containsEntry(MapFieldKeys.SUCCESS, 12L)
                .containsEntry(MapFieldKeys.FAILED, 2L)
                .containsEntry(MapFieldKeys.PAUSED, 1L)
                .containsEntry(MapFieldKeys.UNIQUE_USERS, 6L)
                .containsEntry(MapFieldKeys.SUCCESS_RATE, "80.0%");
        verify(executionMapper, never()).selectList(any());
    }

    private CanvasExecutionStatsDO statsRow(LocalDate statDate,
                                            int total,
                                            int success,
                                            int failed,
                                            int paused,
                                            int uniqueUsers) {
        CanvasExecutionStatsDO row = new CanvasExecutionStatsDO();
        row.setStatDate(statDate);
        row.setTotalCount(total);
        row.setSuccessCount(success);
        row.setFailCount(failed);
        row.setPausedCount(paused);
        row.setUniqueUsers(uniqueUsers);
        return row;
    }
}
