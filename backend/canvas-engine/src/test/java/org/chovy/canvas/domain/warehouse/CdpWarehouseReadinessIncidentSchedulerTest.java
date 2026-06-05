package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.Duration;
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

class CdpWarehouseReadinessIncidentSchedulerTest {

    @Test
    void disabledSchedulerSkipsScan() {
        CdpWarehouseReadinessIncidentService service = mock(CdpWarehouseReadinessIncidentService.class);
        CdpWarehouseReadinessIncidentScheduler scheduler =
                new CdpWarehouseReadinessIncidentScheduler(service, false, 9L);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isFalse();
        verify(service, never()).scan(9L);
    }

    @Test
    void enabledSchedulerRunsScan() {
        CdpWarehouseReadinessIncidentService service = mock(CdpWarehouseReadinessIncidentService.class);
        CdpWarehouseReadinessIncidentScheduler scheduler =
                new CdpWarehouseReadinessIncidentScheduler(service, true, 9L);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isTrue();
        verify(service).scan(9L);
    }

    @Test
    void deniedLeaseSkipsScanAcrossInstances() {
        CdpWarehouseReadinessIncidentService service = mock(CdpWarehouseReadinessIncidentService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(9L), eq("CDP_WAREHOUSE_READINESS_INCIDENT"),
                eq(Duration.ofSeconds(60)), any())).thenReturn(false);
        CdpWarehouseReadinessIncidentScheduler scheduler =
                new CdpWarehouseReadinessIncidentScheduler(service, leaseService, true, 9L, 60);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void overlapGuardSkipsNestedCycle() {
        CdpWarehouseReadinessIncidentService service = mock(CdpWarehouseReadinessIncidentService.class);
        CdpWarehouseReadinessIncidentScheduler scheduler =
                new CdpWarehouseReadinessIncidentScheduler(service, true, 9L);
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle());
            return new CdpWarehouseReadinessIncidentService.ScanResult(9L, "WARN", 5, 1, 4, 0);
        }).when(service).scan(9L);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isTrue();
        assertThat(nestedExecuted).isFalse();
        verify(service).scan(9L);
    }
}
