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

class CdpWarehouseConsumerAvailabilityIncidentSchedulerTest {

    @Test
    void disabledSchedulerSkipsScan() {
        CdpWarehouseConsumerAvailabilityIncidentService service =
                mock(CdpWarehouseConsumerAvailabilityIncidentService.class);
        CdpWarehouseConsumerAvailabilityIncidentScheduler scheduler =
                new CdpWarehouseConsumerAvailabilityIncidentScheduler(service, false, 9L);

        boolean executed = scheduler.runCycle(LocalDateTime.parse("2026-06-05T12:00:00"));

        assertThat(executed).isFalse();
        verify(service, never()).scan(any(), any(), any(), any(), any(), any());
    }

    @Test
    void enabledSchedulerRunsRollingWindowScan() {
        CdpWarehouseConsumerAvailabilityIncidentService service =
                mock(CdpWarehouseConsumerAvailabilityIncidentService.class);
        CdpWarehouseConsumerAvailabilityIncidentScheduler scheduler =
                new CdpWarehouseConsumerAvailabilityIncidentScheduler(
                        service, null, true, 9L, "BI_METRIC", 30, "scheduler", 60);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T12:00:00");

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        verify(service).scan(9L, null, "BI_METRIC", now.minusMinutes(30), now, "scheduler");
    }

    @Test
    void deniedLeaseSkipsScanAcrossInstances() {
        CdpWarehouseConsumerAvailabilityIncidentService service =
                mock(CdpWarehouseConsumerAvailabilityIncidentService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(9L), eq("CDP_WAREHOUSE_CONSUMER_AVAILABILITY_INCIDENT"),
                eq(Duration.ofSeconds(60)), any())).thenReturn(false);
        CdpWarehouseConsumerAvailabilityIncidentScheduler scheduler =
                new CdpWarehouseConsumerAvailabilityIncidentScheduler(
                        service, leaseService, true, 9L, null, 60, "scheduler", 60);

        boolean executed = scheduler.runCycle(LocalDateTime.parse("2026-06-05T12:00:00"));

        assertThat(executed).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void overlapGuardSkipsNestedCycle() {
        CdpWarehouseConsumerAvailabilityIncidentService service =
                mock(CdpWarehouseConsumerAvailabilityIncidentService.class);
        CdpWarehouseConsumerAvailabilityIncidentScheduler scheduler =
                new CdpWarehouseConsumerAvailabilityIncidentScheduler(service, true, 9L);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T12:00:00");
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle(now));
            return new CdpWarehouseConsumerAvailabilityIncidentService.ScanResult(
                    9L, null, null, now.minusHours(1), now, "WARN",
                    2, 1, 0, 1, 0, now, now);
        }).when(service).scan(9L, null, null, now.minusHours(1), now,
                "consumer-availability-incident-scheduler");

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        assertThat(nestedExecuted).isFalse();
    }
}
