package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseRealtimePipelineIncidentServiceTest {

    @Test
    void scanRecordsIncidentsForNonPassPipelinesAndSkipsPassRows() {
        CdpWarehouseRealtimePipelineService pipelineService = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        when(pipelineService.status(9L, 5)).thenReturn(new CdpWarehouseRealtimePipelineService.PipelineStatusSummary(
                9L, 3, 1, 1, 1, List.of(
                runtime("pipeline_pass", "PASS"),
                runtime("pipeline_warn", "WARN"),
                runtime("pipeline_fail", "FAIL"))));
        CdpWarehouseRealtimePipelineIncidentService service =
                new CdpWarehouseRealtimePipelineIncidentService(pipelineService, incidentService);

        CdpWarehouseRealtimePipelineIncidentService.ScanResult result = service.scan(9L, 5);

        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.opened()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        verify(incidentService).recordRealtimePipelineIncident(org.mockito.ArgumentMatchers.argThat(input ->
                "pipeline_warn".equals(input.pipelineKey())
                        && "WARN".equals(input.status())
                        && input.pipelineId().equals(101L)));
        verify(incidentService).recordRealtimePipelineIncident(org.mockito.ArgumentMatchers.argThat(input ->
                "pipeline_fail".equals(input.pipelineKey())
                        && "FAIL".equals(input.status())
                        && input.reasons().contains("reported status is FAIL")));
    }

    @Test
    void scanCountsIncidentWriteFailures() {
        CdpWarehouseRealtimePipelineService pipelineService = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        when(pipelineService.status(9L, 5)).thenReturn(new CdpWarehouseRealtimePipelineService.PipelineStatusSummary(
                9L, 1, 0, 1, 0, List.of(runtime("pipeline_warn", "WARN"))));
        doThrow(new RuntimeException("incident store unavailable"))
                .when(incidentService).recordRealtimePipelineIncident(any());
        CdpWarehouseRealtimePipelineIncidentService service =
                new CdpWarehouseRealtimePipelineIncidentService(pipelineService, incidentService);

        CdpWarehouseRealtimePipelineIncidentService.ScanResult result = service.scan(9L, 5);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.opened()).isZero();
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isEqualTo(1);
    }

    private CdpWarehouseRealtimePipelineService.PipelineRuntimeView runtime(String key, String status) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        return new CdpWarehouseRealtimePipelineService.PipelineRuntimeView(
                new CdpWarehouseRealtimePipelineService.PipelineContractView(
                        101L, 9L, key, key, "MYSQL_CDC", "canvas.table", "canvas.table",
                        "cg", "FLINK_CDC", "DORIS", "canvas_ods.table", "EXACTLY_ONCE",
                        60, 300_000L, 300, "ACTIVE", "data-platform", "{}",
                        "chk-1", "100", "100", now.minusSeconds(1), now,
                        900_000L, status, "message", "flink"),
                status,
                "message",
                "FAIL".equals(status) ? List.of("reported status is FAIL") : List.of("lag exceeds maxLagMs"),
                "chk-1",
                "100",
                "100",
                now.minusSeconds(1),
                now,
                900_000L,
                "flink",
                List.of());
    }
}
