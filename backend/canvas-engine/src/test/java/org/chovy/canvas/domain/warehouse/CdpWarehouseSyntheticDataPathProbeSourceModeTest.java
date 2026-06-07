package org.chovy.canvas.domain.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseSyntheticDataPathProbeRunDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseSyntheticDataPathProbeRunMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseSyntheticDataPathProbeSourceModeTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-05T00:00:00Z"), ZoneId.of("UTC"));

    @Test
    void mysqlCdcRunWritesSourceTableAndDoesNotUseDirectStreamLoadSink() {
        CdpWarehouseSyntheticDataPathProbeRunMapper runMapper = runMapper();
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        CdpEventLogMapper sourceMapper = mock(CdpEventLogMapper.class);
        JdbcTemplate doris = mock(JdbcTemplate.class);
        when(doris.queryForObject(anyString(), eq(Long.class), any(), any(), any())).thenReturn(1L);
        CdpWarehouseSyntheticDataPathProbeService service =
                service(runMapper, provider(sink), provider(doris), provider(sourceMapper));

        CdpWarehouseSyntheticDataPathProbeService.ProbeRunView view = service.run(9L,
                new CdpWarehouseSyntheticDataPathProbeService.RunCommand(
                        "flink-cdc-cert", null, true, 1, 0, "MYSQL_CDC"));

        assertThat(view.status()).isEqualTo("PASS");
        assertThat(view.sourceMode()).isEqualTo("MYSQL_CDC");
        assertThat(view.sourceStatus()).isEqualTo("PASS");
        assertThat(view.sinkStatus()).isEqualTo("SKIPPED");
        assertThat(view.odsStatus()).isEqualTo("PASS");

        ArgumentCaptor<CdpEventLogDO> event = ArgumentCaptor.forClass(CdpEventLogDO.class);
        verify(sourceMapper).insert(event.capture());
        assertThat(event.getValue().getTenantId()).isEqualTo(9L);
        assertThat(event.getValue().getEventCode()).isEqualTo("__warehouse_probe__");
        assertThat(event.getValue().getProperties()).contains("\"probeKey\":\"flink-cdc-cert\"");
        verify(sink, never()).writeAccepted(any());
    }

    @Test
    void strictMysqlCdcRunFailsWhenSourceTableWriterIsMissing() {
        CdpWarehouseSyntheticDataPathProbeRunMapper runMapper = runMapper();
        CdpWarehouseEventSink sink = mock(CdpWarehouseEventSink.class);
        JdbcTemplate doris = mock(JdbcTemplate.class);
        CdpWarehouseSyntheticDataPathProbeService service =
                service(runMapper, provider(sink), provider(doris), provider(null));

        CdpWarehouseSyntheticDataPathProbeService.ProbeRunView view = service.run(9L,
                new CdpWarehouseSyntheticDataPathProbeService.RunCommand(
                        "flink-cdc-cert", null, true, 1, 0, "MYSQL_CDC"));

        assertThat(view.status()).isEqualTo("FAIL");
        assertThat(view.sourceMode()).isEqualTo("MYSQL_CDC");
        assertThat(view.sourceStatus()).isEqualTo("FAIL");
        assertThat(view.sinkStatus()).isEqualTo("SKIPPED");
        assertThat(view.errorMessage()).contains("source cdp_event_log writer");
        verify(sink, never()).writeAccepted(any());
    }

    private CdpWarehouseSyntheticDataPathProbeService service(
            CdpWarehouseSyntheticDataPathProbeRunMapper runMapper,
            ObjectProvider<CdpWarehouseEventSink> sinkProvider,
            ObjectProvider<JdbcTemplate> dorisProvider,
            ObjectProvider<CdpEventLogMapper> sourceMapperProvider) {
        return new CdpWarehouseSyntheticDataPathProbeService(
                runMapper, sinkProvider, dorisProvider, sourceMapperProvider, new ObjectMapper(), CLOCK);
    }

    private CdpWarehouseSyntheticDataPathProbeRunMapper runMapper() {
        CdpWarehouseSyntheticDataPathProbeRunMapper mapper =
                mock(CdpWarehouseSyntheticDataPathProbeRunMapper.class);
        doAnswer(invocation -> {
            CdpWarehouseSyntheticDataPathProbeRunDO row = invocation.getArgument(0);
            row.setId(7L);
            return 1;
        }).when(mapper).insert(org.mockito.ArgumentMatchers.<CdpWarehouseSyntheticDataPathProbeRunDO>any());
        return mapper;
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
