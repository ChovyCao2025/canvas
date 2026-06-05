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

class CdpWarehouseRetentionSchedulerTest {

    @Test
    void disabledSchedulerSkipsCleanup() {
        CdpWarehouseRetentionService service = mock(CdpWarehouseRetentionService.class);
        CdpWarehouseRetentionScheduler scheduler =
                new CdpWarehouseRetentionScheduler(service, false, 9L, 30, 14, 90);

        boolean executed = scheduler.runCycle(LocalDateTime.of(2026, 6, 5, 12, 0));

        assertThat(executed).isFalse();
        verify(service, never()).cleanup(any(), any(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    void enabledSchedulerRunsCleanup() {
        CdpWarehouseRetentionService service = mock(CdpWarehouseRetentionService.class);
        CdpWarehouseRetentionScheduler scheduler =
                new CdpWarehouseRetentionScheduler(service, true, 9L, 30, 14, 90);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        verify(service).cleanup(9L, now, 30, 14, 90, "warehouse-retention-scheduler");
    }

    @Test
    void deniedLeaseSkipsCleanupAcrossInstances() {
        CdpWarehouseRetentionService service = mock(CdpWarehouseRetentionService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(9L), eq("CDP_WAREHOUSE_RETENTION"),
                eq(Duration.ofSeconds(300)), any())).thenReturn(false);
        CdpWarehouseRetentionScheduler scheduler =
                new CdpWarehouseRetentionScheduler(service, leaseService, true, 9L, 30, 14, 90, 300);

        boolean executed = scheduler.runCycle(LocalDateTime.of(2026, 6, 5, 12, 0));

        assertThat(executed).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void overlapGuardSkipsNestedCycle() {
        CdpWarehouseRetentionService service = mock(CdpWarehouseRetentionService.class);
        CdpWarehouseRetentionScheduler scheduler =
                new CdpWarehouseRetentionScheduler(service, true, 9L, 30, 14, 90);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle(now));
            return cleanupResult(now);
        }).when(service).cleanup(9L, now, 30, 14, 90, "warehouse-retention-scheduler");

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        assertThat(nestedExecuted).isFalse();
        verify(service).cleanup(9L, now, 30, 14, 90, "warehouse-retention-scheduler");
    }

    private CdpWarehouseRetentionService.RetentionCleanupResult cleanupResult(LocalDateTime now) {
        CdpWarehouseRetentionService.RetentionTargetResult empty =
                new CdpWarehouseRetentionService.RetentionTargetResult("SYNC_RUNS", 30, now, 0, 0);
        return new CdpWarehouseRetentionService.RetentionCleanupResult(
                9L,
                now,
                "warehouse-retention-scheduler",
                empty,
                empty,
                empty,
                0);
    }
}
