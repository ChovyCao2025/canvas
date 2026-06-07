package org.chovy.canvas.domain.marketing;

import org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingIntegrationContractProbeSchedulerTest {

    @Test
    void runCycleDoesNothingWhenDisabled() {
        MarketingIntegrationContractProbeAutomationService automationService =
                mock(MarketingIntegrationContractProbeAutomationService.class);
        MarketingIntegrationContractProbeScheduler scheduler =
                new MarketingIntegrationContractProbeScheduler(
                        automationService,
                        null,
                        false,
                        7L,
                        50,
                        "probe-scheduler",
                        120);

        assertThat(scheduler.runCycle()).isFalse();

        verify(automationService, never()).scanProductionContracts(any(), any(), any());
    }

    @Test
    void runCycleUsesLeaseAndScansWhenEnabled() {
        MarketingIntegrationContractProbeAutomationService automationService =
                mock(MarketingIntegrationContractProbeAutomationService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(7L), eq("MARKETING_INTEGRATION_CONTRACT_PROBES"), any(Duration.class), any()))
                .thenAnswer(invocation -> invocation.<Supplier<Boolean>>getArgument(3).get());
        when(automationService.scanProductionContracts(7L, 25, "probe-scheduler"))
                .thenReturn(new MarketingIntegrationContractProbeAutomationService.ProbeAutomationSummary(
                        7L, 1, 1, 1, 0, 0, null, List.of()));
        MarketingIntegrationContractProbeScheduler scheduler =
                new MarketingIntegrationContractProbeScheduler(
                        automationService,
                        leaseService,
                        true,
                        7L,
                        25,
                        "probe-scheduler",
                        120);

        assertThat(scheduler.runCycle()).isTrue();

        verify(automationService).scanProductionContracts(7L, 25, "probe-scheduler");
    }

    @Test
    void runCycleSkipsScanWhenLeaseIsDenied() {
        MarketingIntegrationContractProbeAutomationService automationService =
                mock(MarketingIntegrationContractProbeAutomationService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(7L), eq("MARKETING_INTEGRATION_CONTRACT_PROBES"), any(Duration.class), any()))
                .thenReturn(false);
        MarketingIntegrationContractProbeScheduler scheduler =
                new MarketingIntegrationContractProbeScheduler(
                        automationService,
                        leaseService,
                        true,
                        7L,
                        25,
                        "probe-scheduler",
                        120);

        assertThat(scheduler.runCycle()).isFalse();

        verify(automationService, never()).scanProductionContracts(any(), any(), any());
    }

    @Test
    void runCycleSkipsNestedScanWhilePreviousCycleIsStillRunning() {
        MarketingIntegrationContractProbeAutomationService automationService =
                mock(MarketingIntegrationContractProbeAutomationService.class);
        MarketingIntegrationContractProbeScheduler scheduler =
                new MarketingIntegrationContractProbeScheduler(
                        automationService,
                        null,
                        true,
                        7L,
                        25,
                        "probe-scheduler",
                        120);
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle());
            return new MarketingIntegrationContractProbeAutomationService.ProbeAutomationSummary(
                    7L, 1, 1, 1, 0, 0, null, List.of());
        }).when(automationService).scanProductionContracts(7L, 25, "probe-scheduler");

        assertThat(scheduler.runCycle()).isTrue();
        assertThat(nestedExecuted).isFalse();
    }
}
