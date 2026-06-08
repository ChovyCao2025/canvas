package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerLeaseService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BiQuickEngineQueueSchedulerServiceTest {

    @Test
    void runScheduledOnceReturnsEmptyWhenDisabled() {
        BiQuickEngineQueueService queueService = mock(BiQuickEngineQueueService.class);
        BiDeliverySchedulerLeaseService leaseService = mock(BiDeliverySchedulerLeaseService.class);
        BiQuickEngineQueueSchedulerService service = service(queueService, false, leaseService);

        BiQuickEngineQueueSchedulerResult result = service.runScheduledOnce();

        assertThat(result.expired()).isZero();
        assertThat(result.recovered()).isZero();
        assertThat(result.claimed()).isZero();
        assertThat(result.skipped()).isZero();
        verifyNoInteractions(queueService, leaseService);
    }

    @Test
    void runScheduledOnceUsesDistributedLeaseToRecoverStaleClaims() {
        BiQuickEngineQueueService queueService = mock(BiQuickEngineQueueService.class);
        BiDeliverySchedulerLeaseService leaseService = mock(BiDeliverySchedulerLeaseService.class);
        when(leaseService.acquire(7L, "BI_QUICK_ENGINE_QUEUE_RECOVERY_GOLD", Duration.ofSeconds(120)))
                .thenReturn(true);
        when(queueService.recoverStaleClaims(7L, "GOLD", 90))
                .thenReturn(new BiQuickEngineQueueRecoveryResult(1, 2));
        BiQuickEngineQueueSchedulerService service = service(queueService, true, leaseService);

        BiQuickEngineQueueSchedulerResult result = service.runScheduledOnce();

        assertThat(result.expired()).isEqualTo(1);
        assertThat(result.recovered()).isEqualTo(2);
        assertThat(result.claimed()).isZero();
        assertThat(result.skipped()).isZero();
        verify(leaseService).acquire(7L, "BI_QUICK_ENGINE_QUEUE_RECOVERY_GOLD", Duration.ofSeconds(120));
        verify(queueService).recoverStaleClaims(7L, "GOLD", 90);
        verify(leaseService).release(7L, "BI_QUICK_ENGINE_QUEUE_RECOVERY_GOLD");
    }

    @Test
    void runScheduledOnceSkipsRecoveryWhenLeaseIsHeldByAnotherInstance() {
        BiQuickEngineQueueService queueService = mock(BiQuickEngineQueueService.class);
        BiDeliverySchedulerLeaseService leaseService = mock(BiDeliverySchedulerLeaseService.class);
        when(leaseService.acquire(7L, "BI_QUICK_ENGINE_QUEUE_RECOVERY_GOLD", Duration.ofSeconds(120)))
                .thenReturn(false);
        BiQuickEngineQueueSchedulerService service = service(queueService, true, leaseService);

        BiQuickEngineQueueSchedulerResult result = service.runScheduledOnce();

        assertThat(result.expired()).isZero();
        assertThat(result.recovered()).isZero();
        assertThat(result.claimed()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(leaseService).acquire(7L, "BI_QUICK_ENGINE_QUEUE_RECOVERY_GOLD", Duration.ofSeconds(120));
        verify(leaseService, never()).release(7L, "BI_QUICK_ENGINE_QUEUE_RECOVERY_GOLD");
        verifyNoInteractions(queueService);
    }

    @Test
    void runScheduledOnceClaimsFairReadyJobsAfterRecoveryUnderDistributedLease() {
        BiQuickEngineQueueService queueService = mock(BiQuickEngineQueueService.class);
        BiDeliverySchedulerLeaseService leaseService = mock(BiDeliverySchedulerLeaseService.class);
        when(leaseService.acquire(7L, "BI_QUICK_ENGINE_QUEUE_RECOVERY_GOLD", Duration.ofSeconds(120)))
                .thenReturn(true);
        when(queueService.recoverStaleClaims(7L, "GOLD", 90))
                .thenReturn(new BiQuickEngineQueueRecoveryResult(0, 1));
        when(queueService.claimReadyFair("quick-engine-worker", 10))
                .thenReturn(new BiQuickEngineQueueClaimResult(2, 3, List.of()));
        BiQuickEngineQueueSchedulerService service = service(queueService, true, leaseService);

        BiQuickEngineQueueSchedulerResult result = service.runScheduledOnce();

        assertThat(result.expired()).isEqualTo(2);
        assertThat(result.recovered()).isEqualTo(1);
        assertThat(result.claimed()).isEqualTo(3);
        assertThat(result.skipped()).isZero();
        verify(queueService).recoverStaleClaims(7L, "GOLD", 90);
        verify(queueService).claimReadyFair("quick-engine-worker", 10);
        verify(leaseService).release(7L, "BI_QUICK_ENGINE_QUEUE_RECOVERY_GOLD");
    }

    @Test
    void runScheduledOnceReturnsFairWorkerWakeupJobsAcrossTenantPools() {
        BiQuickEngineQueueService queueService = mock(BiQuickEngineQueueService.class);
        BiDeliverySchedulerLeaseService leaseService = mock(BiDeliverySchedulerLeaseService.class);
        LocalDateTime claimedAt = LocalDateTime.of(2026, 6, 9, 10, 30);
        when(leaseService.acquire(7L, "BI_QUICK_ENGINE_QUEUE_RECOVERY_GOLD", Duration.ofSeconds(120)))
                .thenReturn(true);
        when(queueService.recoverStaleClaims(7L, "GOLD", 90))
                .thenReturn(new BiQuickEngineQueueRecoveryResult(0, 0));
        when(queueService.claimReadyFair("quick-engine-worker", 10))
                .thenReturn(new BiQuickEngineQueueClaimResult(0, 3, List.of(
                        job(101L, 7L, "GOLD", "daily_sales", "quick-engine-worker", claimedAt),
                        job(201L, 8L, "SILVER", "customer_orders", "quick-engine-worker", claimedAt),
                        job(102L, 7L, "GOLD", "daily_sales", "quick-engine-worker", claimedAt))));
        BiQuickEngineQueueSchedulerService service = service(queueService, true, leaseService);

        BiQuickEngineQueueSchedulerResult result = service.runScheduledOnce();

        assertThat(result.claimed()).isEqualTo(3);
        assertThat(result.wakeupJobs()).extracting(BiQuickEngineQueueJobView::id)
                .containsExactly(101L, 201L, 102L);
        assertThat(result.wakeupJobs()).extracting(BiQuickEngineQueueJobView::tenantId)
                .containsExactly(7L, 8L, 7L);
        assertThat(result.wakeupJobs()).extracting(BiQuickEngineQueueJobView::poolKey)
                .containsExactly("GOLD", "SILVER", "GOLD");
        assertThat(result.wakeupJobs()).extracting(BiQuickEngineQueueJobView::claimedBy)
                .containsOnly("quick-engine-worker");
    }

    private BiQuickEngineQueueSchedulerService service(BiQuickEngineQueueService queueService,
                                                       boolean enabled,
                                                       BiDeliverySchedulerLeaseService leaseService) {
        return new BiQuickEngineQueueSchedulerService(
                queueService,
                enabled,
                7L,
                "gold",
                90,
                leaseService,
                120);
    }

    private BiQuickEngineQueueJobView job(Long id,
                                          Long tenantId,
                                          String poolKey,
                                          String datasetKey,
                                          String claimedBy,
                                          LocalDateTime claimedAt) {
        return new BiQuickEngineQueueJobView(
                id,
                tenantId,
                poolKey,
                "hash-" + id,
                datasetKey,
                "alice",
                "CLAIMED",
                1,
                claimedAt.minusSeconds(30),
                claimedAt.plusSeconds(90),
                claimedBy,
                claimedAt,
                null,
                null,
                claimedAt.minusSeconds(30),
                claimedAt);
    }
}
