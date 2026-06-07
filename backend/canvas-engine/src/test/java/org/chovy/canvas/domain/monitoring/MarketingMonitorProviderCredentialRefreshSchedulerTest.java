package org.chovy.canvas.domain.monitoring;

import org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingMonitorProviderCredentialRefreshSchedulerTest {

    @Test
    void disabledSchedulerDoesNotRefreshDueCredentials() {
        MarketingMonitorProviderCredentialService service = mock(MarketingMonitorProviderCredentialService.class);
        MarketingMonitorProviderCredentialRefreshScheduler scheduler =
                new MarketingMonitorProviderCredentialRefreshScheduler(
                        service,
                        null,
                        false,
                        7L,
                        30,
                        20,
                        "monitoring-token-scheduler",
                        120);

        assertThat(scheduler.runCycle(LocalDateTime.of(2026, 6, 6, 8, 0))).isFalse();

        verify(service, never()).refreshDue(any(), any(), any());
    }

    @Test
    void enabledSchedulerRefreshesDueCredentialsUnderLease() {
        MarketingMonitorProviderCredentialService service = mock(MarketingMonitorProviderCredentialService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(service.refreshDue(eq(7L), any(), eq("monitoring-token-scheduler")))
                .thenReturn(new MarketingMonitorProviderCredentialDueRefreshResult(
                        7L,
                        1,
                        1,
                        1,
                        0,
                        0,
                        LocalDateTime.of(2026, 6, 6, 8, 30),
                        LocalDateTime.of(2026, 6, 6, 8, 0),
                        List.of()));
        when(leaseService.runWithLease(eq(7L), eq("MARKETING_MONITOR_PROVIDER_CREDENTIAL_REFRESH"),
                eq(Duration.ofSeconds(120)), any()))
                .thenAnswer(invocation -> invocation.<Supplier<Boolean>>getArgument(3).get());
        MarketingMonitorProviderCredentialRefreshScheduler scheduler =
                new MarketingMonitorProviderCredentialRefreshScheduler(
                        service,
                        leaseService,
                        true,
                        7L,
                        30,
                        20,
                        "monitoring-token-scheduler",
                        120);
        ArgumentCaptor<MarketingMonitorProviderCredentialDueRefreshCommand> commandCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialDueRefreshCommand.class);

        assertThat(scheduler.runCycle(LocalDateTime.of(2026, 6, 6, 8, 0))).isTrue();

        verify(service).refreshDue(eq(7L), commandCaptor.capture(), eq("monitoring-token-scheduler"));
        assertThat(commandCaptor.getValue().windowMinutes()).isEqualTo(30);
        assertThat(commandCaptor.getValue().limit()).isEqualTo(20);
    }
}
