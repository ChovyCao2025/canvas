package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseTableDriftIncidentServiceTest {

    @Test
    void scanLiveInspectionsOpensIncidentsForNonPassReports() {
        CdpWarehouseTableGovernanceService governanceService =
                mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        when(governanceService.inspectLiveAll(9L, "qa")).thenReturn(summary(9L, List.of(
                report("canvas_daily_stats", "PASS"),
                report("event_ods", "WARN"))));
        CdpWarehouseTableDriftIncidentService service =
                new CdpWarehouseTableDriftIncidentService(governanceService, incidentService);

        CdpWarehouseTableDriftIncidentService.ScanResult result = service.scan(9L, true, "qa");

        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.live()).isTrue();
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.opened()).isEqualTo(1);
        assertThat(result.resolved()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        verify(incidentService).recordTableDriftIncident(argThat(input ->
                "event_ods".equals(input.tableKey())
                        && "WARN".equals(input.status())
                        && "LIVE:SHOW_CREATE_TABLE".equals(input.inspectionSource())));
        verify(incidentService).resolveTableDriftIncident(9L, "canvas_daily_stats", "qa");
    }

    @Test
    void scanResolvesExistingIncidentForPassingReport() {
        CdpWarehouseTableGovernanceService governanceService =
                mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        when(governanceService.inspectLiveAll(9L, "qa")).thenReturn(summary(9L, List.of(
                report("event_ods", "PASS"))));
        when(incidentService.resolveTableDriftIncident(9L, "event_ods", "qa")).thenReturn(true);
        CdpWarehouseTableDriftIncidentService service =
                new CdpWarehouseTableDriftIncidentService(governanceService, incidentService);

        CdpWarehouseTableDriftIncidentService.ScanResult result = service.scan(9L, true, "qa");

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.opened()).isZero();
        assertThat(result.resolved()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isZero();
        verify(incidentService).resolveTableDriftIncident(9L, "event_ods", "qa");
        verify(incidentService, never()).recordTableDriftIncident(any());
    }

    @Test
    void scanAssetInspectionsUsesAssetInspectionMode() {
        CdpWarehouseTableGovernanceService governanceService =
                mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        when(governanceService.inspectAll(9L, "warehouse-table-drift")).thenReturn(summary(9L, List.of(
                report("event_ods", "FAIL"))));
        CdpWarehouseTableDriftIncidentService service =
                new CdpWarehouseTableDriftIncidentService(governanceService, incidentService);

        CdpWarehouseTableDriftIncidentService.ScanResult result = service.scan(9L, false, null);

        assertThat(result.live()).isFalse();
        assertThat(result.opened()).isEqualTo(1);
        assertThat(result.resolved()).isZero();
        verify(governanceService).inspectAll(9L, "warehouse-table-drift");
    }

    @Test
    void scanCountsIncidentWriteFailuresWithoutStopping() {
        CdpWarehouseTableGovernanceService governanceService =
                mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        when(governanceService.inspectLiveAll(9L, "qa")).thenReturn(summary(9L, List.of(
                report("event_ods", "WARN"),
                report("user_wide", "WARN"))));
        doThrow(new RuntimeException("incident store unavailable"))
                .when(incidentService).recordTableDriftIncident(argThat(input ->
                        "event_ods".equals(input.tableKey())));
        CdpWarehouseTableDriftIncidentService service =
                new CdpWarehouseTableDriftIncidentService(governanceService, incidentService);

        CdpWarehouseTableDriftIncidentService.ScanResult result = service.scan(9L, true, "qa");

        assertThat(result.opened()).isEqualTo(1);
        assertThat(result.resolved()).isZero();
        assertThat(result.failed()).isEqualTo(1);
    }

    private CdpWarehouseTableGovernanceService.InspectionSummary summary(
            Long tenantId,
            List<CdpWarehouseTableGovernanceService.InspectionReport> reports) {
        long passed = reports.stream().filter(report -> "PASS".equals(report.status())).count();
        long warned = reports.stream().filter(report -> "WARN".equals(report.status())).count();
        long failed = reports.stream().filter(report -> "FAIL".equals(report.status())).count();
        return new CdpWarehouseTableGovernanceService.InspectionSummary(
                tenantId, reports.size(), passed, warned, failed, reports);
    }

    private CdpWarehouseTableGovernanceService.InspectionReport report(String tableKey, String status) {
        return new CdpWarehouseTableGovernanceService.InspectionReport(
                51L,
                9L,
                tableKey,
                "canvas_dws." + tableKey,
                status,
                8,
                "PASS".equals(status) ? 0 : 1,
                "PASS".equals(status) ? List.of() : List.of("DDL bucket count is not 8"),
                "PASS".equals(status) ? "Physical table contract passed" : status + " with 1 violation",
                "LIVE:SHOW_CREATE_TABLE",
                LocalDateTime.of(2026, 6, 5, 12, 0));
    }
}
