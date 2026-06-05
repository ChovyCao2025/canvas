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

class CdpWarehouseRealtimeRetrySchedulerTest {

    @Test
    void disabledSchedulerSkipsRetry() {
        CdpWarehouseRealtimeRetryService service = mock(CdpWarehouseRealtimeRetryService.class);
        CdpWarehouseRealtimeRetryScheduler scheduler =
                new CdpWarehouseRealtimeRetryScheduler(service, false, 100, 3);

        boolean executed = scheduler.runCycle(LocalDateTime.of(2026, 6, 5, 12, 0));

        assertThat(executed).isFalse();
        verify(service, never()).retryDue(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void enabledSchedulerRunsBoundedRetry() {
        CdpWarehouseRealtimeRetryService service = mock(CdpWarehouseRealtimeRetryService.class);
        CdpWarehouseRealtimeRetryScheduler scheduler =
                new CdpWarehouseRealtimeRetryScheduler(service, true, 100, 3);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        verify(service).retryDue(now, 100, 3);
    }

    @Test
    void deniedLeaseSkipsRetryAcrossInstances() {
        CdpWarehouseRealtimeRetryService service = mock(CdpWarehouseRealtimeRetryService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(0L), eq("CDP_WAREHOUSE_REALTIME_RETRY"),
                eq(Duration.ofSeconds(60)), any())).thenReturn(false);
        CdpWarehouseRealtimeRetryScheduler scheduler =
                new CdpWarehouseRealtimeRetryScheduler(service, leaseService, true, 0L, 100, 3, 60);

        boolean executed = scheduler.runCycle(LocalDateTime.of(2026, 6, 5, 12, 0));

        assertThat(executed).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void overlapGuardSkipsNestedCycle() {
        CdpWarehouseRealtimeRetryService service = mock(CdpWarehouseRealtimeRetryService.class);
        CdpWarehouseRealtimeRetryScheduler scheduler =
                new CdpWarehouseRealtimeRetryScheduler(service, true, 100, 3);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle(now));
            return new CdpWarehouseRealtimeRetryService.RetryResult(0, 0, 0, 0);
        }).when(service).retryDue(now, 100, 3);

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        assertThat(nestedExecuted).isFalse();
    }
}
