package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.Duration;
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

class CdpWarehouseTableDriftIncidentSchedulerTest {

    @Test
    void disabledSchedulerSkipsScan() {
        CdpWarehouseTableDriftIncidentService service =
                mock(CdpWarehouseTableDriftIncidentService.class);
        CdpWarehouseTableDriftIncidentScheduler scheduler =
                new CdpWarehouseTableDriftIncidentScheduler(service, false, 9L);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isFalse();
        verify(service, never()).scan(9L, true, "warehouse-table-drift-scheduler");
    }

    @Test
    void enabledSchedulerRunsScan() {
        CdpWarehouseTableDriftIncidentService service =
                mock(CdpWarehouseTableDriftIncidentService.class);
        CdpWarehouseTableDriftIncidentScheduler scheduler =
                new CdpWarehouseTableDriftIncidentScheduler(service, true, 9L);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isTrue();
        verify(service).scan(9L, true, "warehouse-table-drift-scheduler");
    }

    @Test
    void deniedLeaseSkipsScanAcrossInstances() {
        CdpWarehouseTableDriftIncidentService service =
                mock(CdpWarehouseTableDriftIncidentService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(9L), eq("CDP_WAREHOUSE_TABLE_DRIFT_INCIDENT"),
                eq(Duration.ofSeconds(300)), any())).thenReturn(false);
        CdpWarehouseTableDriftIncidentScheduler scheduler =
                new CdpWarehouseTableDriftIncidentScheduler(
                        service, leaseService, true, 9L, false, 300);

        boolean executed = scheduler.runCycle();

        assertThat(executed).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void overlapGuardSkipsNestedCycle() {
        CdpWarehouseTableDriftIncidentService service =
                mock(CdpWarehouseTableDriftIncidentService.class);
        CdpWarehouseTableDriftIncidentScheduler scheduler =
                new CdpWarehouseTableDriftIncidentScheduler(service, true, 9L);
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runCycle());
            return new CdpWarehouseTableDriftIncidentService.ScanResult(9L, true, 1, 1, 0, 0, 0);
        }).when(service).scan(9L, true, "warehouse-table-drift-scheduler");

        boolean executed = scheduler.runCycle();

        assertThat(executed).isTrue();
        assertThat(nestedExecuted).isFalse();
        verify(service).scan(9L, true, "warehouse-table-drift-scheduler");
    }
}
