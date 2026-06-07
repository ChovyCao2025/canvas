package org.chovy.canvas.domain.monitoring;

import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingMonitorPollingScheduleServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.parse("2026-06-06T09:00:00");

    @Test
    void pollsDueEnabledSourcesAndSkipsFutureOrDisabledSources() {
        MarketingMonitorSourceMapper sourceMapper = mock(MarketingMonitorSourceMapper.class);
        MarketingMonitorPollingService pollingService = mock(MarketingMonitorPollingService.class);
        when(sourceMapper.selectList(any())).thenReturn(List.of(
                source(10L, 7L, 1, 1, NOW.minusMinutes(1)),
                source(11L, 7L, 1, 1, null),
                source(12L, 7L, 1, 1, NOW.plusMinutes(5)),
                source(13L, 7L, 0, 1, NOW.minusMinutes(1)),
                source(14L, 7L, 1, 0, NOW.minusMinutes(1)),
                source(15L, 8L, 1, 1, NOW.minusMinutes(1))));
        MarketingMonitorPollingScheduleService service =
                new MarketingMonitorPollingScheduleService(sourceMapper, pollingService);

        MarketingMonitorPollingScheduleService.ScheduledPollResult result =
                service.pollDueSources(7L, NOW, 50, "monitoring-scheduler");

        assertThat(result.tenantId()).isEqualTo(7L);
        assertThat(result.candidateCount()).isEqualTo(6);
        assertThat(result.dueCount()).isEqualTo(2);
        assertThat(result.succeededCount()).isEqualTo(2);
        assertThat(result.failedCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(4);
        assertThat(result.evaluatedAt()).isEqualTo(NOW);
        verify(pollingService).pollSource(7L, 10L, scheduledCommand(NOW), "monitoring-scheduler");
        verify(pollingService).pollSource(7L, 11L, scheduledCommand(NOW), "monitoring-scheduler");
        verify(pollingService, never()).pollSource(7L, 12L, scheduledCommand(NOW), "monitoring-scheduler");
        verify(pollingService, never()).pollSource(7L, 13L, scheduledCommand(NOW), "monitoring-scheduler");
        verify(pollingService, never()).pollSource(7L, 14L, scheduledCommand(NOW), "monitoring-scheduler");
        verify(pollingService, never()).pollSource(7L, 15L, scheduledCommand(NOW), "monitoring-scheduler");
    }

    @Test
    void isolatesPerSourceFailuresAndContinuesCycle() {
        MarketingMonitorSourceMapper sourceMapper = mock(MarketingMonitorSourceMapper.class);
        MarketingMonitorPollingService pollingService = mock(MarketingMonitorPollingService.class);
        when(sourceMapper.selectList(any())).thenReturn(List.of(
                source(10L, 7L, 1, 1, NOW.minusMinutes(1)),
                source(11L, 7L, 1, 1, NOW.minusMinutes(2))));
        doThrow(new IllegalStateException("provider outage"))
                .when(pollingService)
                .pollSource(7L, 10L, scheduledCommand(NOW), "monitoring-scheduler");
        MarketingMonitorPollingScheduleService service =
                new MarketingMonitorPollingScheduleService(sourceMapper, pollingService);

        MarketingMonitorPollingScheduleService.ScheduledPollResult result =
                service.pollDueSources(7L, NOW, 50, "monitoring-scheduler");

        assertThat(result.dueCount()).isEqualTo(2);
        assertThat(result.succeededCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        verify(pollingService).pollSource(7L, 11L, scheduledCommand(NOW), "monitoring-scheduler");
    }

    @Test
    void respectsLimitAfterDueFiltering() {
        MarketingMonitorSourceMapper sourceMapper = mock(MarketingMonitorSourceMapper.class);
        MarketingMonitorPollingService pollingService = mock(MarketingMonitorPollingService.class);
        when(sourceMapper.selectList(any())).thenReturn(List.of(
                source(10L, 7L, 1, 1, NOW.minusMinutes(3)),
                source(11L, 7L, 1, 1, NOW.minusMinutes(2)),
                source(12L, 7L, 1, 1, NOW.minusMinutes(1))));
        MarketingMonitorPollingScheduleService service =
                new MarketingMonitorPollingScheduleService(sourceMapper, pollingService);

        MarketingMonitorPollingScheduleService.ScheduledPollResult result =
                service.pollDueSources(7L, NOW, 2, "monitoring-scheduler");

        assertThat(result.dueCount()).isEqualTo(2);
        assertThat(result.succeededCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isEqualTo(1);
        verify(pollingService).pollSource(7L, 10L, scheduledCommand(NOW), "monitoring-scheduler");
        verify(pollingService).pollSource(7L, 11L, scheduledCommand(NOW), "monitoring-scheduler");
        verify(pollingService, never()).pollSource(7L, 12L, scheduledCommand(NOW), "monitoring-scheduler");
    }

    private MarketingMonitorPollCommand scheduledCommand(LocalDateTime now) {
        return new MarketingMonitorPollCommand(null, now, null, 100, false);
    }

    private MarketingMonitorSourceDO source(Long id,
                                            Long tenantId,
                                            Integer enabled,
                                            Integer pollEnabled,
                                            LocalDateTime nextPollAt) {
        MarketingMonitorSourceDO source = new MarketingMonitorSourceDO();
        source.setId(id);
        source.setTenantId(tenantId);
        source.setEnabled(enabled);
        source.setPollEnabled(pollEnabled);
        source.setNextPollAt(nextPollAt);
        return source;
    }
}
