package org.chovy.canvas.domain.analytics;

import org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseService;
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

class CdpWarehouseAudienceMaterializationSchedulerTest {

    @Test
    void disabledSchedulerSkipsCycle() {
        AudienceMaterializationScheduleService service = mock(AudienceMaterializationScheduleService.class);
        CdpWarehouseAudienceMaterializationScheduler scheduler =
                new CdpWarehouseAudienceMaterializationScheduler(service, false, 9L, 20, "scheduler");

        boolean executed = scheduler.runCycle(LocalDateTime.parse("2026-06-05T05:00:00"));

        assertThat(executed).isFalse();
        verify(service, never()).refreshDue(any(), any(), anyInt(), any());
    }

    @Test
    void enabledSchedulerRunsDueRefresh() {
        AudienceMaterializationScheduleService service = mock(AudienceMaterializationScheduleService.class);
        CdpWarehouseAudienceMaterializationScheduler scheduler =
                new CdpWarehouseAudienceMaterializationScheduler(service, true, 9L, 20, "scheduler");
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        verify(service).refreshDue(9L, now, 20, "scheduler");
    }

    @Test
    void enabledSchedulerCanRunAvailabilityGatedRefresh() {
        AudienceMaterializationScheduleService service = mock(AudienceMaterializationScheduleService.class);
        CdpWarehouseAudienceMaterializationScheduler scheduler =
                new CdpWarehouseAudienceMaterializationScheduler(
                        service,
                        null,
                        true,
                        9L,
                        20,
                        "scheduler",
                        120,
                        true,
                        "HYBRID",
                        false);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        verify(service).refreshDueWithAvailabilityGate(9L, now, 20, "scheduler", null, now, "HYBRID", false);
        verify(service, never()).refreshDue(any(), any(), anyInt(), any());
    }

    @Test
    void consumerContractGateTakesPrecedenceOverAvailabilityGate() {
        AudienceMaterializationScheduleService service = mock(AudienceMaterializationScheduleService.class);
        CdpWarehouseAudienceMaterializationScheduler scheduler =
                new CdpWarehouseAudienceMaterializationScheduler(
                        service,
                        null,
                        true,
                        9L,
                        20,
                        "scheduler",
                        120,
                        true,
                        "HYBRID",
                        false,
                        true,
                        "audience_");
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        verify(service).refreshDueWithConsumerAvailabilityContracts(
                9L,
                now,
                20,
                "scheduler",
                null,
                now,
                "audience_");
        verify(service, never()).refreshDueWithAvailabilityGate(any(), any(), anyInt(), any(), any(), any(), any(), any(Boolean.class));
        verify(service, never()).refreshDue(any(), any(), anyInt(), any());
    }

    @Test
    void deniedLeaseSkipsDueRefreshAcrossInstances() {
        AudienceMaterializationScheduleService service = mock(AudienceMaterializationScheduleService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(9L), eq("CDP_AUDIENCE_MATERIALIZATION"),
                eq(Duration.ofSeconds(120)), any())).thenReturn(false);
        CdpWarehouseAudienceMaterializationScheduler scheduler =
                new CdpWarehouseAudienceMaterializationScheduler(service, leaseService, true, 9L, 20, "scheduler", 120);

        boolean executed = scheduler.runCycle(LocalDateTime.parse("2026-06-05T05:00:00"));

        assertThat(executed).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void overlapGuardSkipsNestedCycle() {
        AudienceMaterializationScheduleService service = mock(AudienceMaterializationScheduleService.class);
        CdpWarehouseAudienceMaterializationScheduler scheduler =
                new CdpWarehouseAudienceMaterializationScheduler(service, true, 9L, 20, "scheduler");
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle(now));
            return new AudienceMaterializationScheduleService.ScheduledRefreshResult(9L, 0, 0, 0, 0, 0, now);
        }).when(service).refreshDue(9L, now, 20, "scheduler");

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        assertThat(nestedExecuted).isFalse();
    }
}
