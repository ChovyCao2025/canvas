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

class CdpWarehouseRealtimeJobIncidentSchedulerTest {

    @Test
    void disabledSchedulerSkipsScan() {
        CdpWarehouseRealtimeJobIncidentService service =
                mock(CdpWarehouseRealtimeJobIncidentService.class);
        CdpWarehouseRealtimeJobIncidentScheduler scheduler =
                new CdpWarehouseRealtimeJobIncidentScheduler(service, false, 9L);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isFalse();
        verify(service, never()).scan(9L, null, 300, 50);
    }

    @Test
    void enabledSchedulerRunsScan() {
        CdpWarehouseRealtimeJobIncidentService service =
                mock(CdpWarehouseRealtimeJobIncidentService.class);
        CdpWarehouseRealtimeJobIncidentScheduler scheduler =
                new CdpWarehouseRealtimeJobIncidentScheduler(service, true, 9L);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isTrue();
        verify(service).scan(9L, null, 300, 50);
    }

    @Test
    void deniedLeaseSkipsScanAcrossInstances() {
        CdpWarehouseRealtimeJobIncidentService service =
                mock(CdpWarehouseRealtimeJobIncidentService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(9L), eq("CDP_WAREHOUSE_REALTIME_JOB_INCIDENT"),
                eq(Duration.ofSeconds(60)), any())).thenReturn(false);
        CdpWarehouseRealtimeJobIncidentScheduler scheduler =
                new CdpWarehouseRealtimeJobIncidentScheduler(
                        service, leaseService, true, 9L, "pipe", 120, 20, 60);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void overlapGuardSkipsNestedCycle() {
        CdpWarehouseRealtimeJobIncidentService service =
                mock(CdpWarehouseRealtimeJobIncidentService.class);
        CdpWarehouseRealtimeJobIncidentScheduler scheduler =
                new CdpWarehouseRealtimeJobIncidentScheduler(service, true, 9L);
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle());
            return new CdpWarehouseRealtimeJobIncidentService.ScanResult(9L, 1, 1, 0, 0);
        }).when(service).scan(9L, null, 300, 50);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isTrue();
        assertThat(nestedExecuted).isFalse();
        verify(service).scan(9L, null, 300, 50);
    }
}
