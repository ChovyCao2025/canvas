package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseAvailabilityIncidentServiceTest {

    @Test
    void scanOpensWarnAndFailGatesAndResolvesPassGate() {
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T11:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T12:00:00");
        when(availabilityService.evaluate(9L, from, to, "HYBRID"))
                .thenReturn(decision(from, to,
                        gate("offline_aggregate", "WARN"),
                        gate("realtime_pipelines", "FAIL"),
                        gate("cached_snapshot", "PASS")));
        when(incidentService.resolveAvailabilityIncident(9L, "HYBRID", "cached_snapshot", "qa"))
                .thenReturn(true);
        CdpWarehouseAvailabilityIncidentService service =
                new CdpWarehouseAvailabilityIncidentService(availabilityService, incidentService);

        CdpWarehouseAvailabilityIncidentService.ScanResult result =
                service.scan(9L, from, to, "HYBRID", "qa");

        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.mode()).isEqualTo("HYBRID");
        assertThat(result.availabilityStatus()).isEqualTo("FAIL");
        assertThat(result.totalGates()).isEqualTo(3);
        assertThat(result.opened()).isEqualTo(2);
        assertThat(result.resolved()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isZero();
        verify(incidentService).recordAvailabilityIncident(argThat(input ->
                input.tenantId().equals(9L)
                        && "HYBRID".equals(input.mode())
                        && "offline_aggregate".equals(input.gateKey())
                        && "WARN".equals(input.gateStatus())));
        verify(incidentService).recordAvailabilityIncident(argThat(input ->
                "realtime_pipelines".equals(input.gateKey())
                        && "FAIL".equals(input.gateStatus())));
        verify(incidentService).resolveAvailabilityIncident(9L, "HYBRID", "cached_snapshot", "qa");
    }

    @Test
    void scanCountsIncidentSideEffectFailuresWithoutAbortingRemainingGates() {
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T11:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T12:00:00");
        when(availabilityService.evaluate(9L, from, to, "OFFLINE"))
                .thenReturn(decision(from, to,
                        gate("offline_aggregate", "WARN"),
                        gate("cached_snapshot", "PASS")));
        doThrow(new IllegalStateException("incident store unavailable"))
                .when(incidentService)
                .recordAvailabilityIncident(argThat(input -> "offline_aggregate".equals(input.gateKey())));
        CdpWarehouseAvailabilityIncidentService service =
                new CdpWarehouseAvailabilityIncidentService(availabilityService, incidentService);

        CdpWarehouseAvailabilityIncidentService.ScanResult result =
                service.scan(9L, from, to, "OFFLINE", "");

        assertThat(result.totalGates()).isEqualTo(2);
        assertThat(result.opened()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        verify(incidentService).resolveAvailabilityIncident(
                9L, "HYBRID", "cached_snapshot", "warehouse-availability");
    }

    private CdpWarehouseAvailabilityService.AvailabilityDecision decision(
            LocalDateTime from,
            LocalDateTime to,
            CdpWarehouseAvailabilityService.AvailabilityGate... gates) {
        String status = List.of(gates).stream()
                .anyMatch(gate -> "FAIL".equals(gate.status())) ? "FAIL" : "WARN";
        return new CdpWarehouseAvailabilityService.AvailabilityDecision(
                9L,
                "HYBRID",
                from,
                to,
                to,
                status,
                List.of(gates));
    }

    private CdpWarehouseAvailabilityService.AvailabilityGate gate(String key, String status) {
        return new CdpWarehouseAvailabilityService.AvailabilityGate(
                key,
                status,
                "test " + status,
                LocalDateTime.parse("2026-06-05T11:55:00"),
                5L,
                1);
    }
}
