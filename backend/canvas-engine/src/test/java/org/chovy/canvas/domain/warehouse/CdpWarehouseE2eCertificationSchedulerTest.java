package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
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

class CdpWarehouseE2eCertificationSchedulerTest {

    @Test
    void disabledSchedulerSkipsCertificationRun() {
        CdpWarehouseE2eCertificationRunService runService = mock(CdpWarehouseE2eCertificationRunService.class);
        CdpWarehouseE2eCertificationScheduler scheduler =
                new CdpWarehouseE2eCertificationScheduler(
                        runService, false, 9L, 60, "HYBRID", "", true, "scheduler", 60);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isFalse();
        verify(runService, never()).run(any(), any(), any(), any(), any(),
                any(Boolean.class), any(Boolean.class), any(Boolean.class), any());
    }

    @Test
    void enabledSchedulerDelegatesToRunServiceWithConfiguredWindowAndContracts() {
        CdpWarehouseE2eCertificationRunService runService = mock(CdpWarehouseE2eCertificationRunService.class);
        CdpWarehouseE2eCertificationScheduler scheduler =
                new CdpWarehouseE2eCertificationScheduler(
                        runService, true, 9L, 90, "HYBRID",
                        "bi_daily_active_users,audience_12", true, true, true, "scheduler", 60);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isTrue();
        verify(runService).run(eq(9L),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq("HYBRID"),
                eq(List.of("bi_daily_active_users", "audience_12")),
                eq(true),
                eq(true),
                eq(true),
                eq("scheduler"));
    }

    @Test
    void deniedLeaseSkipsCertificationRun() {
        CdpWarehouseE2eCertificationRunService runService = mock(CdpWarehouseE2eCertificationRunService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(9L), eq("CDP_WAREHOUSE_E2E_CERTIFICATION"),
                eq(Duration.ofSeconds(60)), any())).thenReturn(false);
        CdpWarehouseE2eCertificationScheduler scheduler =
                new CdpWarehouseE2eCertificationScheduler(
                        runService, leaseService, true, 9L, 60, "HYBRID", "", true, "scheduler", 60);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isFalse();
        verifyNoInteractions(runService);
    }

    @Test
    void overlapGuardSkipsNestedCycle() {
        CdpWarehouseE2eCertificationRunService runService = mock(CdpWarehouseE2eCertificationRunService.class);
        CdpWarehouseE2eCertificationScheduler scheduler =
                new CdpWarehouseE2eCertificationScheduler(
                        runService, true, 9L, 60, "HYBRID", "", true, "scheduler", 60);
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle());
            return null;
        }).when(runService).run(any(), any(), any(), any(), any(),
                any(Boolean.class), any(Boolean.class), any(Boolean.class), any());

        boolean executed = scheduler.runCycle();

        assertThat(executed).isTrue();
        assertThat(nestedExecuted).isFalse();
    }
}
