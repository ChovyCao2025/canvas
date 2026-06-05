package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
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

class CdpWarehouseExternalRealtimeJobProbeSchedulerTest {

    @Test
    void disabledSchedulerSkipsScan() {
        CdpWarehouseExternalRealtimeJobProbeService service =
                mock(CdpWarehouseExternalRealtimeJobProbeService.class);
        CdpWarehouseExternalRealtimeJobProbeScheduler scheduler =
                new CdpWarehouseExternalRealtimeJobProbeScheduler(service, false, 9L);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isFalse();
        verify(service, never()).scan(eq(9L), any());
    }

    @Test
    void enabledSchedulerRunsScan() {
        CdpWarehouseExternalRealtimeJobProbeService service =
                mock(CdpWarehouseExternalRealtimeJobProbeService.class);
        when(service.scan(eq(9L), any())).thenReturn(scanSummary());
        CdpWarehouseExternalRealtimeJobProbeScheduler scheduler =
                new CdpWarehouseExternalRealtimeJobProbeScheduler(service, true, 9L);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isTrue();
        verify(service).scan(eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                command.targetId() == null && command.limit() == 50));
    }

    @Test
    void deniedLeaseSkipsScanAcrossInstances() {
        CdpWarehouseExternalRealtimeJobProbeService service =
                mock(CdpWarehouseExternalRealtimeJobProbeService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(9L), eq("CDP_WAREHOUSE_EXTERNAL_REALTIME_JOB_PROBE"),
                eq(Duration.ofSeconds(60)), any())).thenReturn(false);
        CdpWarehouseExternalRealtimeJobProbeScheduler scheduler =
                new CdpWarehouseExternalRealtimeJobProbeScheduler(service, leaseService, true, 9L, 20, 60);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void overlapGuardSkipsNestedCycle() {
        CdpWarehouseExternalRealtimeJobProbeService service =
                mock(CdpWarehouseExternalRealtimeJobProbeService.class);
        CdpWarehouseExternalRealtimeJobProbeScheduler scheduler =
                new CdpWarehouseExternalRealtimeJobProbeScheduler(service, true, 9L);
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle());
            return scanSummary();
        }).when(service).scan(eq(9L), any());

        boolean executed = scheduler.runCycle();

        assertThat(executed).isTrue();
        assertThat(nestedExecuted).isFalse();
    }

    private CdpWarehouseExternalRealtimeJobProbeService.ScanSummary scanSummary() {
        return new CdpWarehouseExternalRealtimeJobProbeService.ScanSummary(
                9L, 1, 1, 0, 0, List.of());
    }
}
