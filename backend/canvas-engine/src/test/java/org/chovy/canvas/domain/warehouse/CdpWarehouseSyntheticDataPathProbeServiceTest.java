package org.chovy.canvas.domain.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseSyntheticDataPathProbeRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseSyntheticDataPathProbeRunMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseSyntheticDataPathProbeServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-05T00:00:00Z"), ZoneId.of("UTC"));

    @Test
    void passWhenSinkWritesAndOdsReadFindsSyntheticEvent() {
        CdpWarehouseSyntheticDataPathProbeRunMapper mapper = mapper();
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        JdbcTemplate doris = mock(JdbcTemplate.class);
        when(doris.queryForObject(anyString(), eq(Long.class), any(), any(), any())).thenReturn(1L);
        CdpWarehouseSyntheticDataPathProbeService service =
                service(mapper, provider(sink), provider(doris));

        CdpWarehouseSyntheticDataPathProbeService.ProbeRunView view = service.run(9L,
                new CdpWarehouseSyntheticDataPathProbeService.RunCommand(
                        "ods-cert", null, true, 1, 0, null));

        assertThat(view.status()).isEqualTo("PASS");
        assertThat(view.sinkStatus()).isEqualTo("PASS");
        assertThat(view.odsStatus()).isEqualTo("PASS");
        assertThat(view.odsRowCount()).isEqualTo(1L);
        assertThat(view.eventCode()).isEqualTo("__warehouse_probe__");
        ArgumentCaptor<CdpEventLogDO> event = ArgumentCaptor.forClass(CdpEventLogDO.class);
        verify(sink).writeAccepted(event.capture());
        assertThat(event.getValue().getTenantId()).isEqualTo(9L);
        assertThat(event.getValue().getEventCode()).isEqualTo("__warehouse_probe__");
        assertThat(event.getValue().getProperties()).contains("\"synthetic\":true");
        assertThat(event.getValue().getReceivedAt()).isEqualTo(now());
        ArgumentCaptor<CdpWarehouseSyntheticDataPathProbeRunDO> completion =
                ArgumentCaptor.forClass(CdpWarehouseSyntheticDataPathProbeRunDO.class);
        verify(mapper).updateCompletion(completion.capture());
        assertThat(completion.getValue().getStatus()).isEqualTo("PASS");
        assertThat(completion.getValue().getEvidenceJson()).contains("ods_read");
    }

    @Test
    void strictRunFailsWithoutDorisJdbcAndDoesNotWriteSyntheticEvent() {
        CdpWarehouseSyntheticDataPathProbeRunMapper mapper = mapper();
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        CdpWarehouseSyntheticDataPathProbeService service =
                service(mapper, provider(sink), provider(null));

        CdpWarehouseSyntheticDataPathProbeService.ProbeRunView view = service.run(9L,
                new CdpWarehouseSyntheticDataPathProbeService.RunCommand(
                        "ods-cert", null, true, 1, 0, null));

        assertThat(view.status()).isEqualTo("FAIL");
        assertThat(view.sinkStatus()).isEqualTo("SKIPPED");
        assertThat(view.odsStatus()).isEqualTo("FAIL");
        assertThat(view.errorMessage()).contains("Doris JDBC");
        verify(sink, never()).writeAccepted(any());
    }

    @Test
    void sinkFailureFailsStrictRun() {
        CdpWarehouseSyntheticDataPathProbeRunMapper mapper = mapper();
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        doThrow(new IllegalStateException("stream load failed")).when(sink).writeAccepted(any());
        JdbcTemplate doris = mock(JdbcTemplate.class);
        CdpWarehouseSyntheticDataPathProbeService service =
                service(mapper, provider(sink), provider(doris));

        CdpWarehouseSyntheticDataPathProbeService.ProbeRunView view = service.run(9L,
                new CdpWarehouseSyntheticDataPathProbeService.RunCommand(
                        "ods-cert", null, true, 1, 0, null));

        assertThat(view.status()).isEqualTo("FAIL");
        assertThat(view.sinkStatus()).isEqualTo("FAIL");
        assertThat(view.odsStatus()).isNull();
        assertThat(view.errorMessage()).contains("stream load failed");
    }

    @Test
    void dryRunWarnsWhenOdsReadFindsNoRows() {
        CdpWarehouseSyntheticDataPathProbeRunMapper mapper = mapper();
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        JdbcTemplate doris = mock(JdbcTemplate.class);
        when(doris.queryForObject(anyString(), eq(Long.class), any(), any(), any())).thenReturn(0L);
        CdpWarehouseSyntheticDataPathProbeService service =
                service(mapper, provider(sink), provider(doris));

        CdpWarehouseSyntheticDataPathProbeService.ProbeRunView view = service.run(9L,
                new CdpWarehouseSyntheticDataPathProbeService.RunCommand(
                        "ods-cert", null, false, 1, 0, null));

        assertThat(view.status()).isEqualTo("WARN");
        assertThat(view.sinkStatus()).isEqualTo("PASS");
        assertThat(view.odsStatus()).isEqualTo("WARN");
        assertThat(view.odsRowCount()).isZero();
    }

    @Test
    void recentReturnsBoundedRows() {
        CdpWarehouseSyntheticDataPathProbeRunMapper mapper = mapper();
        when(mapper.listRecent(9L, 100)).thenReturn(List.of(runRow("PASS")));
        CdpWarehouseSyntheticDataPathProbeService service =
                service(mapper, provider(null), provider(null));

        List<CdpWarehouseSyntheticDataPathProbeService.ProbeRunView> recent =
                service.recent(9L, 500);

        assertThat(recent).singleElement()
                .satisfies(row -> assertThat(row.status()).isEqualTo("PASS"));
        verify(mapper).listRecent(9L, 100);
    }

    private CdpWarehouseSyntheticDataPathProbeRunMapper mapper() {
        CdpWarehouseSyntheticDataPathProbeRunMapper mapper =
                mock(CdpWarehouseSyntheticDataPathProbeRunMapper.class);
        doAnswer(invocation -> {
            CdpWarehouseSyntheticDataPathProbeRunDO row = invocation.getArgument(0);
            row.setId(7L);
            return 1;
        }).when(mapper).insert(org.mockito.ArgumentMatchers.<CdpWarehouseSyntheticDataPathProbeRunDO>any());
        return mapper;
    }

    private CdpWarehouseSyntheticDataPathProbeService service(
            CdpWarehouseSyntheticDataPathProbeRunMapper mapper,
            ObjectProvider<CdpWarehouseEventSink> sinkProvider,
            ObjectProvider<JdbcTemplate> dorisProvider) {
        return new CdpWarehouseSyntheticDataPathProbeService(
                mapper, sinkProvider, dorisProvider, provider(null), new ObjectMapper(), CLOCK);
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }

    private CdpWarehouseSyntheticDataPathProbeRunDO runRow(String status) {
        CdpWarehouseSyntheticDataPathProbeRunDO row = new CdpWarehouseSyntheticDataPathProbeRunDO();
        row.setId(7L);
        row.setTenantId(9L);
        row.setProbeKey("ods-cert");
        row.setMessageId("warehouse-probe-1");
        row.setEventCode("__warehouse_probe__");
        row.setUserId("__warehouse_probe_user_1");
        row.setStrictMode(1);
        row.setStatus(status);
        row.setSinkStatus(status);
        row.setOdsStatus(status);
        row.setOdsRowCount(1L);
        row.setStartedAt(now());
        row.setFinishedAt(now());
        return row;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }
}
