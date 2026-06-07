package org.chovy.canvas.domain.monitoring;

import org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Constructor;
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

class MarketingMonitorPollingSchedulerTest {

    @Test
    void schedulerIsDisabledByDefaultAndMustBeOptedIn() {
        assertThat(autowiredValue("${canvas.monitoring.polling-scheduler.enabled:"))
                .isEqualTo("${canvas.monitoring.polling-scheduler.enabled:false}");
    }

    @Test
    void disabledSchedulerSkipsCycle() {
        MarketingMonitorPollingScheduleService service = mock(MarketingMonitorPollingScheduleService.class);
        MarketingMonitorPollingScheduler scheduler =
                new MarketingMonitorPollingScheduler(service, false, 7L, 20, "scheduler");

        boolean executed = scheduler.runCycle(LocalDateTime.parse("2026-06-06T09:00:00"));

        assertThat(executed).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void enabledSchedulerRunsDuePollingCycle() {
        MarketingMonitorPollingScheduleService service = mock(MarketingMonitorPollingScheduleService.class);
        MarketingMonitorPollingScheduler scheduler =
                new MarketingMonitorPollingScheduler(service, true, 7L, 20, "scheduler");
        LocalDateTime now = LocalDateTime.parse("2026-06-06T09:00:00");

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        verify(service).pollDueSources(7L, now, 20, "scheduler");
    }

    @Test
    void deniedLeaseSkipsCycleAcrossInstances() {
        MarketingMonitorPollingScheduleService service = mock(MarketingMonitorPollingScheduleService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(7L), eq("MARKETING_MONITOR_POLLING"),
                eq(Duration.ofSeconds(120)), any())).thenReturn(false);
        MarketingMonitorPollingScheduler scheduler =
                new MarketingMonitorPollingScheduler(service, leaseService, true, 7L, 20, "scheduler", 120);

        boolean executed = scheduler.runCycle(LocalDateTime.parse("2026-06-06T09:00:00"));

        assertThat(executed).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void overlapGuardSkipsNestedCycle() {
        MarketingMonitorPollingScheduleService service = mock(MarketingMonitorPollingScheduleService.class);
        MarketingMonitorPollingScheduler scheduler =
                new MarketingMonitorPollingScheduler(service, true, 7L, 20, "scheduler");
        LocalDateTime now = LocalDateTime.parse("2026-06-06T09:00:00");
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle(now));
            return new MarketingMonitorPollingScheduleService.ScheduledPollResult(
                    7L,
                    0,
                    0,
                    0,
                    0,
                    0,
                    now);
        }).when(service).pollDueSources(7L, now, 20, "scheduler");

        boolean executed = scheduler.runCycle(now);

        assertThat(executed).isTrue();
        assertThat(nestedExecuted).isFalse();
    }

    private String autowiredValue(String prefix) {
        for (Constructor<?> constructor : MarketingMonitorPollingScheduler.class.getConstructors()) {
            for (java.lang.reflect.Parameter parameter : constructor.getParameters()) {
                Value value = parameter.getAnnotation(Value.class);
                if (value != null && value.value().startsWith(prefix)) {
                    return value.value();
                }
            }
        }
        throw new AssertionError("missing @Value for " + prefix);
    }
}
