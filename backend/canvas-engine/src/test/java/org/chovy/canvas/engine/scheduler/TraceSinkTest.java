package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.dal.dataobject.AnalyticsEventDO;
import org.chovy.canvas.dal.dataobject.AnalyticsEventTraceDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.mapper.AnalyticsEventMapper;
import org.chovy.canvas.dal.mapper.AnalyticsEventTraceMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.chovy.canvas.domain.analytics.MySqlTraceEventSink;
import org.chovy.canvas.domain.analytics.TraceEventSink;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraceSinkTest {

    @Test
    void migrationProvidesAnalyticsSinkAndRetentionFields() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V132__analytics_event_trace_schema_and_sink.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS analytics_event")
                .contains("tenant_id")
                .contains("session_id")
                .contains("platform")
                .contains("device_type")
                .contains("event_time")
                .contains("received_at")
                .contains("schema_version")
                .contains("business_value")
                .contains("retention_class")
                .contains("archive_status")
                .contains("legal_hold")
                .contains("CREATE TABLE IF NOT EXISTS analytics_event_trace")
                .contains("idx_analytics_trace_archive")
                .contains("idx_analytics_trace_retention")
                .doesNotContain("DROP TABLE");
    }

    @Test
    void mysqlSinkWritesTraceBatchAndExposesCounters() {
        CanvasExecutionTraceMapper traceMapper = mock(CanvasExecutionTraceMapper.class);
        AnalyticsEventTraceMapper analyticsTraceMapper = mock(AnalyticsEventTraceMapper.class);
        AnalyticsEventMapper analyticsEventMapper = mock(AnalyticsEventMapper.class);
        MySqlTraceEventSink sink = new MySqlTraceEventSink(traceMapper, analyticsTraceMapper, analyticsEventMapper);

        sink.writeTraces(List.of(trace("exec-1", "node-1")));

        verify(traceMapper).insertBatch(argThat(list -> list.size() == 1));
        verify(analyticsTraceMapper).insert(any(AnalyticsEventTraceDO.class));
        assertThat(sink.metrics().writtenCount()).isEqualTo(1);
        assertThat(sink.metrics().failedCount()).isZero();
    }

    @Test
    void mysqlSinkCountsFailuresWithoutThrowing() {
        CanvasExecutionTraceMapper traceMapper = mock(CanvasExecutionTraceMapper.class);
        doThrow(new IllegalStateException("mysql unavailable")).when(traceMapper).insertBatch(anyList());
        MySqlTraceEventSink sink = new MySqlTraceEventSink(
                traceMapper,
                mock(AnalyticsEventTraceMapper.class),
                mock(AnalyticsEventMapper.class));

        assertThatCode(() -> sink.writeTraces(List.of(trace("exec-2", "node-1"))))
                .doesNotThrowAnyException();

        assertThat(sink.metrics().writtenCount()).isZero();
        assertThat(sink.metrics().failedCount()).isEqualTo(1);
    }

    @Test
    void mysqlSinkWritesAnalyticsEvents() {
        CanvasExecutionTraceMapper traceMapper = mock(CanvasExecutionTraceMapper.class);
        AnalyticsEventMapper analyticsEventMapper = mock(AnalyticsEventMapper.class);
        MySqlTraceEventSink sink = new MySqlTraceEventSink(
                traceMapper,
                mock(AnalyticsEventTraceMapper.class),
                analyticsEventMapper);

        sink.writeEvents(List.of(AnalyticsEventDO.builder()
                .tenantId(7L)
                .eventCode("checkout")
                .userId("user-1")
                .eventTime(LocalDateTime.parse("2026-06-09T10:15:30"))
                .build()));

        verify(analyticsEventMapper).insert(argThat((AnalyticsEventDO row) ->
                row.getTenantId().equals(7L) && row.getEventCode().equals("checkout")));
        assertThat(sink.metrics().writtenCount()).isEqualTo(1);
        assertThat(sink.metrics().failedCount()).isZero();
    }

    @Test
    void traceWriteBufferUsesSinkAndCountsDroppedRowsWhenFull() {
        TraceEventSink sink = mock(TraceEventSink.class);
        when(sink.metrics()).thenReturn(new TraceEventSink.SinkMetrics(0, 0, 0, 0));
        TraceWriteBuffer buffer = new TraceWriteBuffer(sink, 1, 1);

        buffer.offer(trace("exec-3", "node-1"));
        buffer.offer(trace("exec-4", "node-2"));

        assertThat(buffer.metrics().droppedCount()).isEqualTo(1);
        assertThat(buffer.metrics().backlog()).isEqualTo(1);

        buffer.flush();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CanvasExecutionTraceDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(sink).writeTraces(captor.capture());
        assertThat(captor.getValue()).extracting(CanvasExecutionTraceDO::getExecutionId)
                .containsExactly("exec-3");
        assertThat(buffer.metrics().backlog()).isZero();
    }

    private CanvasExecutionTraceDO trace(String executionId, String nodeId) {
        return CanvasExecutionTraceDO.builder()
                .tenantId(7L)
                .executionId(executionId)
                .nodeId(nodeId)
                .nodeType("TEST")
                .nodeName("Test Node")
                .status(1)
                .startedAt(LocalDateTime.parse("2026-06-09T10:15:30"))
                .finishedAt(LocalDateTime.parse("2026-06-09T10:15:31"))
                .durationMs(1000L)
                .build();
    }
}
