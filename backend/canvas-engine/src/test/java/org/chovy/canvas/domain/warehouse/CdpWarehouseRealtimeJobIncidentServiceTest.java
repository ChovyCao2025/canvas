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

class CdpWarehouseRealtimeJobIncidentServiceTest {

    @Test
    void scanRecordsIncidentsForNonPassJobsAndSkipsPassRows() {
        CdpWarehouseRealtimeJobControlService jobService =
                mock(CdpWarehouseRealtimeJobControlService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        when(jobService.status(9L, "pipe", 120, 20))
                .thenReturn(new CdpWarehouseRealtimeJobControlService.JobStatusSummary(
                        9L, 3, 1, 1, 1, List.of(
                        job("job-pass", "PASS"),
                        job("job-warn", "WARN"),
                        job("job-fail", "FAIL"))));
        CdpWarehouseRealtimeJobIncidentService service =
                new CdpWarehouseRealtimeJobIncidentService(jobService, incidentService);

        CdpWarehouseRealtimeJobIncidentService.ScanResult result =
                service.scan(9L, "pipe", 120, 20);

        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.opened()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        verify(incidentService).recordRealtimeJobIncident(org.mockito.ArgumentMatchers.argThat(input ->
                "job-warn".equals(input.jobKey())
                        && "WARN".equals(input.healthStatus())
                        && input.jobInstanceId().equals(101L)));
        verify(incidentService).recordRealtimeJobIncident(org.mockito.ArgumentMatchers.argThat(input ->
                "job-fail".equals(input.jobKey())
                        && "FAIL".equals(input.healthStatus())
                        && input.reasons().contains("job runtime status is FAILED")));
    }

    @Test
    void scanCountsIncidentWriteFailures() {
        CdpWarehouseRealtimeJobControlService jobService =
                mock(CdpWarehouseRealtimeJobControlService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        when(jobService.status(9L, null, 300, 50))
                .thenReturn(new CdpWarehouseRealtimeJobControlService.JobStatusSummary(
                        9L, 1, 0, 1, 0, List.of(job("job-warn", "WARN"))));
        doThrow(new RuntimeException("incident store unavailable"))
                .when(incidentService).recordRealtimeJobIncident(any());
        CdpWarehouseRealtimeJobIncidentService service =
                new CdpWarehouseRealtimeJobIncidentService(jobService, incidentService);

        CdpWarehouseRealtimeJobIncidentService.ScanResult result =
                service.scan(9L, null, 300, 50);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.opened()).isZero();
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isEqualTo(1);
    }

    private CdpWarehouseRealtimeJobControlService.JobInstanceView job(String jobKey, String status) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        return new CdpWarehouseRealtimeJobControlService.JobInstanceView(
                101L,
                9L,
                "pipe",
                jobKey,
                "FLINK",
                "flink-1",
                "deployment/flink-1",
                "FAIL".equals(status) ? "FAILED" : "RUNNING",
                "RUNNING",
                now.minusSeconds(30),
                "{}",
                "FAIL".equals(status) ? "job failed" : null,
                "data-platform",
                status,
                "FAIL".equals(status)
                        ? List.of("job runtime status is FAILED")
                        : List.of("heartbeat age 600s exceeds maxHeartbeatAgeSeconds 300"),
                now.minusHours(1),
                now.minusSeconds(30));
    }
}
