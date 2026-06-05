package org.chovy.canvas.domain.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseExternalRealtimeJobProbeTargetDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseExternalRealtimeJobProbeTargetMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CdpWarehouseExternalRealtimeJobProbeServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-05T00:00:00Z"), ZoneId.of("UTC"));

    @Test
    void passingProbeWritesRunningHeartbeat() {
        CdpWarehouseExternalRealtimeJobProbeTargetMapper mapper = mock(CdpWarehouseExternalRealtimeJobProbeTargetMapper.class);
        CdpWarehouseExternalRealtimeJobProbeClient client = mock(CdpWarehouseExternalRealtimeJobProbeClient.class);
        CdpWarehouseRealtimeJobControlService jobControl = mock(CdpWarehouseRealtimeJobControlService.class);
        when(mapper.listEnabledTargets(9L, 50)).thenReturn(List.of(target(1L, 1)));
        when(client.probe(any())).thenReturn(new CdpWarehouseExternalRealtimeJobProbeClient.ProbeResult(
                "RUNNING", "flink job running", "{\"state\":\"RUNNING\"}", "flink-1"));
        when(jobControl.heartbeat(eq(9L), any())).thenReturn(jobView("RUNNING", "PASS"));
        CdpWarehouseExternalRealtimeJobProbeService service = service(mapper, client, jobControl);

        CdpWarehouseExternalRealtimeJobProbeService.ScanSummary summary =
                service.scan(9L, new CdpWarehouseExternalRealtimeJobProbeService.ScanCommand(null, 50));

        assertThat(summary.total()).isEqualTo(1);
        assertThat(summary.passed()).isEqualTo(1);
        assertThat(summary.failed()).isZero();
        assertThat(summary.results().get(0).runtimeStatus()).isEqualTo("RUNNING");
        ArgumentCaptor<CdpWarehouseRealtimeJobControlService.HeartbeatCommand> command =
                ArgumentCaptor.forClass(CdpWarehouseRealtimeJobControlService.HeartbeatCommand.class);
        verify(jobControl).heartbeat(eq(9L), command.capture());
        assertThat(command.getValue().pipelineKey()).isEqualTo("pipe");
        assertThat(command.getValue().jobKey()).isEqualTo("job-a");
        assertThat(command.getValue().engineType()).isEqualTo("FLINK_REST");
        assertThat(command.getValue().engineJobId()).isEqualTo("flink-1");
        assertThat(command.getValue().runtimeStatus()).isEqualTo("RUNNING");
        assertThat(command.getValue().heartbeatAt()).isEqualTo(now());
        assertThat(command.getValue().heartbeatPayloadJson()).contains("\"state\":\"RUNNING\"");
        verify(mapper).updateProbeResult(9L, 1L, now(), "PASS", "flink job running");
    }

    @Test
    void failedProbeWritesFailedHeartbeat() {
        CdpWarehouseExternalRealtimeJobProbeTargetMapper mapper = mock(CdpWarehouseExternalRealtimeJobProbeTargetMapper.class);
        CdpWarehouseExternalRealtimeJobProbeClient client = mock(CdpWarehouseExternalRealtimeJobProbeClient.class);
        CdpWarehouseRealtimeJobControlService jobControl = mock(CdpWarehouseRealtimeJobControlService.class);
        when(mapper.listEnabledTargets(9L, 50)).thenReturn(List.of(target(1L, 1)));
        when(client.probe(any())).thenThrow(new IllegalStateException("connect refused"));
        when(jobControl.heartbeat(eq(9L), any())).thenReturn(jobView("FAILED", "FAIL"));
        CdpWarehouseExternalRealtimeJobProbeService service = service(mapper, client, jobControl);

        CdpWarehouseExternalRealtimeJobProbeService.ScanSummary summary =
                service.scan(9L, new CdpWarehouseExternalRealtimeJobProbeService.ScanCommand(null, 50));

        assertThat(summary.total()).isEqualTo(1);
        assertThat(summary.failed()).isEqualTo(1);
        assertThat(summary.results().get(0).runtimeStatus()).isEqualTo("FAILED");
        ArgumentCaptor<CdpWarehouseRealtimeJobControlService.HeartbeatCommand> command =
                ArgumentCaptor.forClass(CdpWarehouseRealtimeJobControlService.HeartbeatCommand.class);
        verify(jobControl).heartbeat(eq(9L), command.capture());
        assertThat(command.getValue().runtimeStatus()).isEqualTo("FAILED");
        assertThat(command.getValue().errorMessage()).isEqualTo("connect refused");
        assertThat(command.getValue().heartbeatPayloadJson()).contains("EXTERNAL_REALTIME_JOB_PROBE");
        verify(mapper).updateProbeResult(9L, 1L, now(), "FAIL", "connect refused");
    }

    @Test
    void disabledTargetIsSkippedWithoutHeartbeat() {
        CdpWarehouseExternalRealtimeJobProbeTargetMapper mapper = mock(CdpWarehouseExternalRealtimeJobProbeTargetMapper.class);
        CdpWarehouseExternalRealtimeJobProbeClient client = mock(CdpWarehouseExternalRealtimeJobProbeClient.class);
        CdpWarehouseRealtimeJobControlService jobControl = mock(CdpWarehouseRealtimeJobControlService.class);
        CdpWarehouseExternalRealtimeJobProbeTargetDO disabled = target(1L, 0);
        when(mapper.findByTenantAndId(9L, 1L)).thenReturn(disabled);
        CdpWarehouseExternalRealtimeJobProbeService service = service(mapper, client, jobControl);

        CdpWarehouseExternalRealtimeJobProbeService.ScanSummary summary =
                service.scan(9L, new CdpWarehouseExternalRealtimeJobProbeService.ScanCommand(1L, 50));

        assertThat(summary.total()).isEqualTo(1);
        assertThat(summary.skipped()).isEqualTo(1);
        assertThat(summary.results().get(0).status()).isEqualTo("SKIPPED");
        verifyNoInteractions(client);
        verifyNoInteractions(jobControl);
        verify(mapper, never()).updateProbeResult(any(), any(), any(), any(), any());
    }

    @Test
    void upsertNormalizesTargetAndReturnsSavedRow() {
        CdpWarehouseExternalRealtimeJobProbeTargetMapper mapper = mock(CdpWarehouseExternalRealtimeJobProbeTargetMapper.class);
        CdpWarehouseExternalRealtimeJobProbeTargetDO saved = target(7L, 1);
        when(mapper.findByKey(9L, "pipe", "job-a")).thenReturn(saved);
        CdpWarehouseExternalRealtimeJobProbeService service =
                service(mapper, mock(CdpWarehouseExternalRealtimeJobProbeClient.class),
                        mock(CdpWarehouseRealtimeJobControlService.class));

        CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView view = service.upsertTarget(9L,
                new CdpWarehouseExternalRealtimeJobProbeService.TargetCommand(
                        " pipe ", "job-a", "flink_rest", " http://flink/jobs/flink-1 ",
                        "secret/flink", "flink-1", null, null, true,
                        "data-platform", 120, "{\"cluster\":\"staging\"}"));

        assertThat(view.id()).isEqualTo(7L);
        assertThat(view.engineType()).isEqualTo("FLINK_REST");
        ArgumentCaptor<CdpWarehouseExternalRealtimeJobProbeTargetDO> row =
                ArgumentCaptor.forClass(CdpWarehouseExternalRealtimeJobProbeTargetDO.class);
        verify(mapper).upsert(row.capture());
        assertThat(row.getValue().getPipelineKey()).isEqualTo("pipe");
        assertThat(row.getValue().getEndpointUrl()).isEqualTo("http://flink/jobs/flink-1");
        assertThat(row.getValue().getMaxStalenessSeconds()).isEqualTo(120);
    }

    private CdpWarehouseExternalRealtimeJobProbeService service(
            CdpWarehouseExternalRealtimeJobProbeTargetMapper mapper,
            CdpWarehouseExternalRealtimeJobProbeClient client,
            CdpWarehouseRealtimeJobControlService jobControl) {
        return new CdpWarehouseExternalRealtimeJobProbeService(
                mapper, client, jobControl, new ObjectMapper(), CLOCK);
    }

    private CdpWarehouseExternalRealtimeJobProbeTargetDO target(Long id, int enabled) {
        CdpWarehouseExternalRealtimeJobProbeTargetDO row = new CdpWarehouseExternalRealtimeJobProbeTargetDO();
        row.setId(id);
        row.setTenantId(9L);
        row.setPipelineKey("pipe");
        row.setJobKey("job-a");
        row.setEngineType("FLINK_REST");
        row.setEndpointUrl("http://flink/jobs/flink-1");
        row.setExternalJobId("flink-1");
        row.setDeploymentRef("flink/jobs/flink-1");
        row.setEnabled(enabled);
        row.setOwnerName("data-platform");
        row.setMaxStalenessSeconds(300);
        return row;
    }

    private CdpWarehouseRealtimeJobControlService.JobInstanceView jobView(String runtimeStatus, String healthStatus) {
        return new CdpWarehouseRealtimeJobControlService.JobInstanceView(
                1L,
                9L,
                "pipe",
                "job-a",
                "FLINK_REST",
                "flink-1",
                "flink/jobs/flink-1",
                runtimeStatus,
                "RUNNING",
                now(),
                "{}",
                null,
                "data-platform",
                healthStatus,
                List.of(),
                null,
                null);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }
}
