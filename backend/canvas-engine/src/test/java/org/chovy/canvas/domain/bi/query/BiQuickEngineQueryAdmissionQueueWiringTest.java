package org.chovy.canvas.domain.bi.query;

import org.chovy.canvas.domain.bi.dataset.BiQuickEngineAdmissionDecision;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityService;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineConcurrencyQueueView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueAdmissionCommand;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueJobView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueService;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineTenantPoolPolicyView;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiQuickEngineQueryAdmissionQueueWiringTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-05T03:00:00Z"), ZoneOffset.UTC);

    @Test
    void persistsAndCompletesDurableQueueJobWhenQuickEngineAdmissionWaitsInQueue() {
        List<BiQueryHistoryEntry> history = new ArrayList<>();
        BiQuickEngineCapacityService capacityService = mock(BiQuickEngineCapacityService.class);
        BiQuickEngineQueueService queueService = mock(BiQuickEngineQueueService.class);
        when(capacityService.admitQueryOrWait(7L, 50)).thenReturn(queuedDecision());
        when(queueService.enqueue(eq(7L), org.mockito.ArgumentMatchers.any(BiQuickEngineQueueAdmissionCommand.class)))
                .thenReturn(queueJob(91L));
        BiQueryExecutionService service = service(
                capacityService,
                queueService,
                history,
                (query, dataset) -> List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L)));

        BiQueryResult result = service.execute(defaultRequest(), new BiQueryContext(7L, "alice"));

        ArgumentCaptor<BiQuickEngineQueueAdmissionCommand> commandCaptor =
                ArgumentCaptor.forClass(BiQuickEngineQueueAdmissionCommand.class);
        verify(queueService).enqueue(eq(7L), commandCaptor.capture());
        BiQuickEngineQueueAdmissionCommand command = commandCaptor.getValue();
        assertThat(command.poolKey()).isEqualTo("GOLD");
        assertThat(command.sqlHash()).isEqualTo(result.sqlHash());
        assertThat(command.datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(command.requestedBy()).isEqualTo("alice");
        assertThat(command.queueTimeoutSeconds()).isEqualTo(120);
        assertThat(history).extracting(BiQueryHistoryEntry::status)
                .containsExactly("QUEUED", "SUCCESS");
        verify(queueService).completeQueuedAdmission(7L, 91L);
        verify(capacityService).releaseQuery(7L);
    }

    @Test
    void blocksDurableQueueJobWhenQueuedQuickEngineExecutionFails() {
        List<BiQueryHistoryEntry> history = new ArrayList<>();
        BiQuickEngineCapacityService capacityService = mock(BiQuickEngineCapacityService.class);
        BiQuickEngineQueueService queueService = mock(BiQuickEngineQueueService.class);
        when(capacityService.admitQueryOrWait(7L, 50)).thenReturn(queuedDecision());
        when(queueService.enqueue(eq(7L), org.mockito.ArgumentMatchers.any(BiQuickEngineQueueAdmissionCommand.class)))
                .thenReturn(queueJob(92L));
        BiQueryExecutionService service = service(
                capacityService,
                queueService,
                history,
                (query, dataset) -> {
                    throw new IllegalStateException("warehouse unavailable");
                });

        assertThatThrownBy(() -> service.execute(defaultRequest(), new BiQueryContext(7L, "alice")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("warehouse unavailable");

        assertThat(history).extracting(BiQueryHistoryEntry::status)
                .containsExactly("QUEUED", "FAILED");
        verify(queueService).blockQueuedAdmission(7L, 92L, "warehouse unavailable");
        verify(capacityService).releaseQuery(7L);
    }

    private BiQueryExecutionService service(BiQuickEngineCapacityService capacityService,
                                            BiQuickEngineQueueService queueService,
                                            List<BiQueryHistoryEntry> history,
                                            BiQueryExecutor executor) {
        return new BiQueryExecutionService(
                new BiQueryCompiler(),
                executor,
                history::add,
                BiQueryResultCache.noop(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                null,
                null,
                CLOCK,
                null,
                null,
                null,
                null,
                null,
                capacityService,
                queueService);
    }

    private BiQuickEngineAdmissionDecision queuedDecision() {
        return new BiQuickEngineAdmissionDecision(
                true,
                "ADMITTED_AFTER_QUEUE",
                "Quick Engine tenant pool GOLD admitted after queued wait",
                new BiQuickEngineTenantPoolPolicyView("GOLD", 2, 10, 120, 100, "ops", null),
                new BiQuickEngineConcurrencyQueueView(1, 1, 0, 0, 0, 50.0, 10.0, "WARNING"));
    }

    private BiQueryRequest defaultRequest() {
        return new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                100
        );
    }

    private BiQuickEngineQueueJobView queueJob(Long id) {
        return new BiQuickEngineQueueJobView(
                id,
                7L,
                "GOLD",
                "hash",
                "canvas_daily_stats",
                "alice",
                "QUEUED",
                0,
                LocalDateTime.of(2026, 6, 5, 3, 0),
                LocalDateTime.of(2026, 6, 5, 3, 2),
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 6, 5, 3, 0),
                LocalDateTime.of(2026, 6, 5, 3, 0));
    }
}
