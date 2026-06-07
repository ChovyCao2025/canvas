package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseEnterpriseOlapEvidenceSchedulerTest {

    @Test
    void disabledSchedulerDoesNotRunCollection() {
        CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService =
                mock(CdpWarehouseEnterpriseOlapEvidenceCollectionService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        CdpWarehouseEnterpriseOlapEvidenceScheduler scheduler =
                new CdpWarehouseEnterpriseOlapEvidenceScheduler(
                        collectionService, leaseService, false, 9L, "SCHEDULED",
                        "enterprise-olap-evidence-scheduler", 60);

        boolean ran = scheduler.runCycle();

        assertThat(ran).isFalse();
        verify(collectionService, never()).run(any(), any(), any());
        verify(leaseService, never()).runWithLease(any(), any(), any(), any());
    }

    @Test
    void enabledSchedulerRunsCollectionUnderLease() {
        CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService =
                mock(CdpWarehouseEnterpriseOlapEvidenceCollectionService.class);
        CdpWarehouseJobLeaseService leaseService = mock(CdpWarehouseJobLeaseService.class);
        when(leaseService.runWithLease(eq(9L), eq("CDP_WAREHOUSE_ENTERPRISE_OLAP_EVIDENCE"),
                any(Duration.class), any())).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(3);
            return work.get();
        });
        CdpWarehouseEnterpriseOlapEvidenceScheduler scheduler =
                new CdpWarehouseEnterpriseOlapEvidenceScheduler(
                        collectionService, leaseService, true, 9L, "SCHEDULED",
                        "enterprise-olap-evidence-scheduler", 120);

        boolean ran = scheduler.runCycle();

        assertThat(ran).isTrue();
        verify(collectionService).run(9L, "SCHEDULED", "enterprise-olap-evidence-scheduler");
    }

    @Test
    void schedulerPreventsOverlappingExecution() {
        CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService =
                mock(CdpWarehouseEnterpriseOlapEvidenceCollectionService.class);
        AtomicReference<CdpWarehouseEnterpriseOlapEvidenceScheduler> schedulerRef = new AtomicReference<>();
        when(collectionService.run(eq(9L), eq("SCHEDULED"), eq("scheduler"))).thenAnswer(invocation -> {
            assertThat(schedulerRef.get().runCycle()).isFalse();
            return null;
        });
        CdpWarehouseEnterpriseOlapEvidenceScheduler scheduler =
                new CdpWarehouseEnterpriseOlapEvidenceScheduler(
                        collectionService, null, true, 9L, "SCHEDULED", "scheduler", 60);
        schedulerRef.set(scheduler);

        boolean ran = scheduler.runCycle();

        assertThat(ran).isTrue();
        verify(collectionService).run(9L, "SCHEDULED", "scheduler");
    }
}
