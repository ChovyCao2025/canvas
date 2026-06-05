package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseReadinessIncidentServiceTest {

    @Test
    void scanRecordsNonPassReadinessSectionsAndSkipsIncidentSection() {
        CdpWarehouseReadinessService readinessService = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        when(readinessService.readiness(9L)).thenReturn(readiness("FAIL", List.of(
                section("offline_sync", "WARN", "offline watermarks are missing"),
                section("realtime_pipelines", "PASS", "healthy"),
                section("incidents", "FAIL", "1 critical open warehouse incident"),
                section("bi_datasources", "FAIL", "1 datasource unavailable"),
                section("audience_materialization", "PASS", "healthy"))));
        CdpWarehouseReadinessIncidentService service =
                new CdpWarehouseReadinessIncidentService(readinessService, incidentService);

        CdpWarehouseReadinessIncidentService.ScanResult result = service.scan(9L);

        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.readinessStatus()).isEqualTo("FAIL");
        assertThat(result.totalSections()).isEqualTo(5);
        assertThat(result.opened()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(3);
        assertThat(result.failed()).isZero();
        verify(incidentService).recordReadinessIncident(argThat(input ->
                "offline_sync".equals(input.sectionKey())
                        && "WARN".equals(input.sectionStatus())
                        && "FAIL".equals(input.readinessStatus())));
        verify(incidentService).recordReadinessIncident(argThat(input ->
                "bi_datasources".equals(input.sectionKey())
                        && "FAIL".equals(input.sectionStatus())
                        && input.reason().contains("unavailable")));
    }

    @Test
    void scanCountsIncidentWriteFailuresAndContinues() {
        CdpWarehouseReadinessService readinessService = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        when(readinessService.readiness(9L)).thenReturn(readiness("WARN", List.of(
                section("offline_sync", "WARN", "offline watermarks are missing"),
                section("audience_materialization", "WARN", "no recent runs"))));
        doThrow(new RuntimeException("incident store unavailable"))
                .when(incidentService).recordReadinessIncident(any());
        CdpWarehouseReadinessIncidentService service =
                new CdpWarehouseReadinessIncidentService(readinessService, incidentService);

        CdpWarehouseReadinessIncidentService.ScanResult result = service.scan(9L);

        assertThat(result.totalSections()).isEqualTo(2);
        assertThat(result.opened()).isZero();
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isEqualTo(2);
    }

    @Test
    void scanHandlesMissingReadinessSummaryDefensively() {
        CdpWarehouseReadinessService readinessService = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        when(readinessService.readiness(null)).thenReturn(null);
        CdpWarehouseReadinessIncidentService service =
                new CdpWarehouseReadinessIncidentService(readinessService, incidentService);

        CdpWarehouseReadinessIncidentService.ScanResult result = service.scan(null);

        assertThat(result.tenantId()).isZero();
        assertThat(result.readinessStatus()).isEqualTo("UNKNOWN");
        assertThat(result.totalSections()).isZero();
        assertThat(result.opened()).isZero();
    }

    private CdpWarehouseReadinessService.ReadinessSummary readiness(
            String status,
            List<CdpWarehouseReadinessService.ReadinessSection> sections) {
        return new CdpWarehouseReadinessService.ReadinessSummary(
                9L,
                status,
                LocalDateTime.of(2026, 6, 5, 12, 0),
                sections,
                new CdpWarehouseReadinessService.OfflineReadiness(status, "ok", 1, 0, 0, 1),
                new CdpWarehouseReadinessService.RealtimeReadiness(status, "ok", 1, 1, 0, 0),
                new CdpWarehouseReadinessService.IncidentReadiness(status, "ok", 0, 0, 0),
                new CdpWarehouseReadinessService.BiReadiness(status, "ok", 1, 1, 0),
                new CdpWarehouseReadinessService.AudienceMaterializationReadiness(status, "ok", 1, 1, 0));
    }

    private CdpWarehouseReadinessService.ReadinessSection section(
            String key,
            String status,
            String reason) {
        return new CdpWarehouseReadinessService.ReadinessSection(key, status, reason);
    }
}
