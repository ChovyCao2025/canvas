package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CdpWarehouseSchedulerTest {

    @Test
    void disabledSchedulerSkipsCycle() {
        CdpWarehouseOperationsService operations = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseScheduler scheduler = new CdpWarehouseScheduler(operations, false, 9L, 100, 30);

        boolean executed = scheduler.runCycle(LocalDateTime.of(2026, 6, 5, 12, 0));

        assertThat(executed).isFalse();
        verify(operations, never()).runScheduledOfflineCycle(any(), any(), anyInt(), anyInt());
    }

    @Test
    void enabledSchedulerRunsOfflineCycle() {
        CdpWarehouseOperationsService operations = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseScheduler scheduler = new CdpWarehouseScheduler(operations, true, 9L, 100, 30);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        verify(operations).runScheduledOfflineCycle(9L, now, 100, 30);
    }

    @Test
    void deniedLeaseSkipsCycleAcrossInstances() {
        CdpWarehouseOperationsService operations = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(9L), eq("CDP_WAREHOUSE_MAIN"),
                eq(Duration.ofSeconds(120)), any())).thenReturn(false);
        CdpWarehouseScheduler scheduler =
                new CdpWarehouseScheduler(operations, leaseService, true, 9L, 100, 30, 120);

        boolean executed = scheduler.runCycle(LocalDateTime.of(2026, 6, 5, 12, 0));

        assertThat(executed).isFalse();
        verifyNoInteractions(operations);
    }

    @Test
    void overlapGuardSkipsNestedCycle() {
        CdpWarehouseOperationsService operations = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseScheduler scheduler = new CdpWarehouseScheduler(operations, true, 9L, 100, 30);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle(now));
            return new CdpWarehouseOperationsService.OfflineCycleResult(
                    1L, 9L, "SUCCESS", 0, 0, null, java.util.List.of());
        }).when(operations).runScheduledOfflineCycle(9L, now, 100, 30);

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        assertThat(nestedExecuted).isFalse();
        verify(operations).runScheduledOfflineCycle(9L, now, 100, 30);
    }
}
