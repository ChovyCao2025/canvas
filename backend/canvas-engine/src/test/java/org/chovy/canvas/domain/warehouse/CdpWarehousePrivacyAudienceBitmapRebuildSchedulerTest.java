package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest {

    @Test
    void springConstructorAcceptsRunHistoryServiceForScheduledPersistence() {
        boolean hasSpringRunHistoryConstructor = Arrays.stream(
                        CdpWarehousePrivacyAudienceBitmapRebuildScheduler.class.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Autowired.class))
                .anyMatch(constructor -> Arrays.asList(constructor.getParameterTypes())
                        .contains(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.class));

        assertThat(hasSpringRunHistoryConstructor).isTrue();
    }

    @Test
    void disabledSchedulerDoesNotRunAutomation() {
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildScheduler scheduler =
                new CdpWarehousePrivacyAudienceBitmapRebuildScheduler(
                        automationService, leaseService, false, 9L, 20, 50, false,
                        "privacy-rebuild-scheduler", 60);

        boolean ran = scheduler.runCycle();

        assertThat(ran).isFalse();
        verify(automationService, never()).run(any(), any());
        verify(leaseService, never()).runWithLease(any(), any(), any(), any());
    }

    @Test
    void enabledSchedulerRunsAutomationUnderLease() {
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult(
                        9L, "PASS", 1, 1, 1, 0, 0, List.of());
        when(automationService.run(eq(9L), any())).thenReturn(result);
        when(leaseService.runWithLease(eq(9L), eq("CDP_WAREHOUSE_PRIVACY_AUDIENCE_REBUILD"),
                any(Duration.class), any())).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(3);
            return work.get();
        });
        CdpWarehousePrivacyAudienceBitmapRebuildScheduler scheduler =
                new CdpWarehousePrivacyAudienceBitmapRebuildScheduler(
                        automationService, leaseService, true, 9L, 20, 50, true,
                        "privacy-rebuild-scheduler", 120);

        boolean ran = scheduler.runCycle();

        assertThat(ran).isTrue();
        ArgumentCaptor<CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand> commandCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand.class);
        verify(automationService).run(eq(9L), commandCaptor.capture());
        assertThat(commandCaptor.getValue().scanLimit()).isEqualTo(20);
        assertThat(commandCaptor.getValue().audienceLimit()).isEqualTo(50);
        assertThat(commandCaptor.getValue().retryFailed()).isTrue();
        assertThat(commandCaptor.getValue().actor()).isEqualTo("privacy-rebuild-scheduler");
    }

    @Test
    void enabledSchedulerRecordsRunHistoryWhenConfigured() {
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService runService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult(
                        9L, "PASS", 1, 1, 1, 0, 0, List.of());
        when(runService.runAndRecord(eq(9L), any(), eq("SCHEDULED")))
                .thenReturn(new CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.AutomationRunView(
                        201L, 9L, "SCHEDULED", "PASS", "privacy-rebuild-scheduler",
                        20, 50, true, 1, 1, 1, 0, 0,
                        "{\"status\":\"PASS\"}", null, null, null, null, null, result));
        when(leaseService.runWithLease(eq(9L), eq("CDP_WAREHOUSE_PRIVACY_AUDIENCE_REBUILD"),
                any(Duration.class), any())).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(3);
            return work.get();
        });
        CdpWarehousePrivacyAudienceBitmapRebuildScheduler scheduler =
                new CdpWarehousePrivacyAudienceBitmapRebuildScheduler(
                        automationService, runService, leaseService, true, 9L, 20, 50, true,
                        "privacy-rebuild-scheduler", 120);

        boolean ran = scheduler.runCycle();

        assertThat(ran).isTrue();
        ArgumentCaptor<CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand> commandCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand.class);
        verify(runService).runAndRecord(eq(9L), commandCaptor.capture(), eq("SCHEDULED"));
        verify(automationService, never()).run(any(), any());
        assertThat(commandCaptor.getValue().actor()).isEqualTo("privacy-rebuild-scheduler");
        assertThat(commandCaptor.getValue().scanLimit()).isEqualTo(20);
        assertThat(commandCaptor.getValue().audienceLimit()).isEqualTo(50);
        assertThat(commandCaptor.getValue().retryFailed()).isTrue();
    }

    @Test
    void schedulerPreventsOverlappingExecution() {
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.class);
        AtomicReference<CdpWarehousePrivacyAudienceBitmapRebuildScheduler> schedulerRef =
                new AtomicReference<>();
        when(automationService.run(eq(9L), any())).thenAnswer(invocation -> {
            assertThat(schedulerRef.get().runCycle()).isFalse();
            return new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult(
                    9L, "PASS", 1, 1, 1, 0, 0, List.of());
        });
        CdpWarehousePrivacyAudienceBitmapRebuildScheduler scheduler =
                new CdpWarehousePrivacyAudienceBitmapRebuildScheduler(
                        automationService, null, true, 9L, 20, 50, false,
                        "privacy-rebuild-scheduler", 60);
        schedulerRef.set(scheduler);

        boolean ran = scheduler.runCycle();

        assertThat(ran).isTrue();
        verify(automationService).run(eq(9L), any());
    }
}
