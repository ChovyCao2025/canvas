package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CdpWarehouseAvailabilityIncidentSchedulerTest {

    @Test
    void disabledSchedulerSkipsScan() {
        CdpWarehouseAvailabilityIncidentService service = mock(CdpWarehouseAvailabilityIncidentService.class);
        CdpWarehouseAvailabilityIncidentScheduler scheduler =
                new CdpWarehouseAvailabilityIncidentScheduler(service, false, 9L);

        boolean executed = scheduler.runCycle(LocalDateTime.parse("2026-06-05T12:00:00"));

        assertThat(executed).isFalse();
        verify(service, never()).scan(any(), any(), any(), any(), any());
    }

    @Test
    void enabledSchedulerRunsRollingWindowScan() {
        CdpWarehouseAvailabilityIncidentService service = mock(CdpWarehouseAvailabilityIncidentService.class);
        CdpWarehouseAvailabilityIncidentScheduler scheduler =
                new CdpWarehouseAvailabilityIncidentScheduler(
                        service, null, true, 9L, "HYBRID", 30, "scheduler", 60);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T12:00:00");

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        verify(service).scan(9L, now.minusMinutes(30), now, "HYBRID", "scheduler");
    }

    @Test
    void deniedLeaseSkipsScanAcrossInstances() {
        CdpWarehouseAvailabilityIncidentService service = mock(CdpWarehouseAvailabilityIncidentService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(9L), eq("CDP_WAREHOUSE_AVAILABILITY_INCIDENT"),
                eq(Duration.ofSeconds(60)), any())).thenReturn(false);
        CdpWarehouseAvailabilityIncidentScheduler scheduler =
                new CdpWarehouseAvailabilityIncidentScheduler(
                        service, leaseService, true, 9L, "HYBRID", 60, "scheduler", 60);

        boolean executed = scheduler.runCycle(LocalDateTime.parse("2026-06-05T12:00:00"));

        assertThat(executed).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void overlapGuardSkipsNestedCycle() {
        CdpWarehouseAvailabilityIncidentService service = mock(CdpWarehouseAvailabilityIncidentService.class);
        CdpWarehouseAvailabilityIncidentScheduler scheduler =
                new CdpWarehouseAvailabilityIncidentScheduler(service, true, 9L);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T12:00:00");
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle(now));
            return new CdpWarehouseAvailabilityIncidentService.ScanResult(
                    9L, "HYBRID", now.minusHours(1), now, "WARN", 2, 1, 0, 1, 0);
        }).when(service).scan(9L, now.minusHours(1), now, "HYBRID", "availability-incident-scheduler");

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        assertThat(nestedExecuted).isFalse();
    }
}
