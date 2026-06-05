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

class CdpWarehouseQualitySchedulerTest {

    @Test
    void disabledSchedulerSkipsChecks() {
        CdpWarehouseQualityService service = mock(CdpWarehouseQualityService.class);
        CdpWarehouseQualityScheduler scheduler =
                new CdpWarehouseQualityScheduler(service, false, 9L, 60, 0, 30);

        boolean executed = scheduler.runCycle(LocalDateTime.of(2026, 6, 5, 12, 0));

        assertThat(executed).isFalse();
        verify(service, never()).reconcileOds(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void enabledSchedulerRunsRecentWindowReconciliationAndLagCheck() {
        CdpWarehouseQualityService service = mock(CdpWarehouseQualityService.class);
        CdpWarehouseQualityScheduler scheduler =
                new CdpWarehouseQualityScheduler(service, true, 9L, 60, 1, 30);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        verify(service).reconcileOds(9L, now.minusMinutes(60), now, 1, "warehouse-quality-scheduler");
        verify(service).checkAggregateLag(9L, now, 30, "warehouse-quality-scheduler");
    }

    @Test
    void deniedLeaseSkipsChecksAcrossInstances() {
        CdpWarehouseQualityService service = mock(CdpWarehouseQualityService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(9L), eq("CDP_WAREHOUSE_QUALITY"),
                eq(Duration.ofSeconds(600)), any())).thenReturn(false);
        CdpWarehouseQualityScheduler scheduler =
                new CdpWarehouseQualityScheduler(service, leaseService, true, 9L, 60, 1, 30, 600);

        boolean executed = scheduler.runCycle(LocalDateTime.of(2026, 6, 5, 12, 0));

        assertThat(executed).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void overlapGuardSkipsNestedCycle() {
        CdpWarehouseQualityService service = mock(CdpWarehouseQualityService.class);
        CdpWarehouseQualityScheduler scheduler =
                new CdpWarehouseQualityScheduler(service, true, 9L, 60, 1, 30);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle(now));
            return new CdpWarehouseQualityService.QualityCheckResult(
                    1L, 9L, "ODS_COUNT", "PASS", 10L, 10L, 0L,
                    now.minusMinutes(60), now, 1L, "{}", now, "warehouse-quality-scheduler");
        }).when(service).reconcileOds(9L, now.minusMinutes(60), now, 1, "warehouse-quality-scheduler");

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        assertThat(nestedExecuted).isFalse();
    }
}
