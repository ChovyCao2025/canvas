package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerLeaseService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
public class BiQuickEngineQueueSchedulerService {

    private static final String LEASE_KEY_PREFIX = "BI_QUICK_ENGINE_QUEUE_RECOVERY_";
    private static final String DEFAULT_POOL_KEY = "STANDARD";
    private static final String DEFAULT_WORKER_ID = "quick-engine-worker";
    private static final int DEFAULT_STALE_CLAIM_SECONDS = 120;
    private static final int DEFAULT_CLAIM_LIMIT = 10;

    private final BiQuickEngineQueueService queueService;
    private final boolean enabled;
    private final Long tenantId;
    private final String poolKey;
    private final int staleClaimSeconds;
    private final String workerId;
    private final int claimLimit;
    private final BiDeliverySchedulerLeaseService leaseService;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public BiQuickEngineQueueSchedulerService(BiQuickEngineQueueService queueService) {
        this(queueService,
                false,
                0L,
                DEFAULT_POOL_KEY,
                DEFAULT_STALE_CLAIM_SECONDS,
                DEFAULT_WORKER_ID,
                DEFAULT_CLAIM_LIMIT,
                (BiDeliverySchedulerLeaseService) null,
                120);
    }

    @Autowired
    public BiQuickEngineQueueSchedulerService(
            BiQuickEngineQueueService queueService,
            @Value("${canvas.bi.quick-engine.queue.scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.bi.quick-engine.queue.scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.bi.quick-engine.queue.scheduler.pool-key:STANDARD}") String poolKey,
            @Value("${canvas.bi.quick-engine.queue.scheduler.stale-claim-seconds:120}") int staleClaimSeconds,
            @Value("${canvas.bi.quick-engine.queue.scheduler.worker-id:quick-engine-worker}") String workerId,
            @Value("${canvas.bi.quick-engine.queue.scheduler.claim-limit:10}") int claimLimit,
            ObjectProvider<BiDeliverySchedulerLeaseService> leaseServiceProvider,
            @Value("${canvas.bi.quick-engine.queue.scheduler.lease-ttl-seconds:120}") long leaseTtlSeconds) {
        this(queueService,
                enabled,
                tenantId,
                poolKey,
                staleClaimSeconds,
                workerId,
                claimLimit,
                leaseServiceProvider == null ? null : leaseServiceProvider.getIfAvailable(),
                leaseTtlSeconds);
    }

    public BiQuickEngineQueueSchedulerService(BiQuickEngineQueueService queueService,
                                              boolean enabled,
                                              Long tenantId,
                                              String poolKey,
                                              int staleClaimSeconds,
                                              BiDeliverySchedulerLeaseService leaseService,
                                              long leaseTtlSeconds) {
        this(queueService,
                enabled,
                tenantId,
                poolKey,
                staleClaimSeconds,
                DEFAULT_WORKER_ID,
                DEFAULT_CLAIM_LIMIT,
                leaseService,
                leaseTtlSeconds);
    }

    public BiQuickEngineQueueSchedulerService(BiQuickEngineQueueService queueService,
                                              boolean enabled,
                                              Long tenantId,
                                              String poolKey,
                                              int staleClaimSeconds,
                                              String workerId,
                                              int claimLimit,
                                              BiDeliverySchedulerLeaseService leaseService,
                                              long leaseTtlSeconds) {
        this.queueService = queueService;
        this.enabled = enabled;
        this.tenantId = tenantId == null ? 0L : tenantId;
        this.poolKey = normalizePoolKey(poolKey);
        this.staleClaimSeconds = staleClaimSeconds <= 0 ? DEFAULT_STALE_CLAIM_SECONDS : staleClaimSeconds;
        this.workerId = workerId == null || workerId.isBlank() ? DEFAULT_WORKER_ID : workerId.trim();
        this.claimLimit = claimLimit <= 0 ? DEFAULT_CLAIM_LIMIT : claimLimit;
        this.leaseService = leaseService;
        this.leaseTtlSeconds = Math.max(1L, leaseTtlSeconds);
    }

    @Scheduled(fixedDelayString = "${canvas.bi.quick-engine.queue.scheduler.fixed-delay-ms:60000}")
    public void scheduledCycle() {
        runScheduledOnce();
    }

    public BiQuickEngineQueueSchedulerResult runScheduledOnce() {
        if (!enabled) {
            return empty();
        }
        if (leaseService == null) {
            return runMaintenanceOnce(tenantId, poolKey, staleClaimSeconds, workerId, claimLimit);
        }
        String leaseKey = leaseKey(poolKey);
        if (!leaseService.acquire(tenantId, leaseKey, Duration.ofSeconds(leaseTtlSeconds))) {
            return new BiQuickEngineQueueSchedulerResult(0, 0, 0, 1);
        }
        try {
            return runMaintenanceOnce(tenantId, poolKey, staleClaimSeconds, workerId, claimLimit);
        } finally {
            leaseService.release(tenantId, leaseKey);
        }
    }

    public BiQuickEngineQueueSchedulerResult runRecoveryOnce(Long tenantId,
                                                             String poolKey,
                                                             int staleClaimSeconds) {
        if (!running.compareAndSet(false, true)) {
            return new BiQuickEngineQueueSchedulerResult(0, 0, 0, 1);
        }
        try {
            BiQuickEngineQueueRecoveryResult recovery = queueService.recoverStaleClaims(
                    tenantId == null ? 0L : tenantId,
                    normalizePoolKey(poolKey),
                    staleClaimSeconds <= 0 ? DEFAULT_STALE_CLAIM_SECONDS : staleClaimSeconds);
            return new BiQuickEngineQueueSchedulerResult(recovery.expired(), recovery.recovered(), 0, 0);
        } finally {
            running.set(false);
        }
    }

    public BiQuickEngineQueueSchedulerResult runMaintenanceOnce(Long tenantId,
                                                                String poolKey,
                                                                int staleClaimSeconds,
                                                                String workerId,
                                                                int claimLimit) {
        if (!running.compareAndSet(false, true)) {
            return new BiQuickEngineQueueSchedulerResult(0, 0, 0, 1);
        }
        try {
            BiQuickEngineQueueRecoveryResult recovery = queueService.recoverStaleClaims(
                    tenantId == null ? 0L : tenantId,
                    normalizePoolKey(poolKey),
                    staleClaimSeconds <= 0 ? DEFAULT_STALE_CLAIM_SECONDS : staleClaimSeconds);
            BiQuickEngineQueueClaimResult claim = queueService.claimReadyFair(
                    workerId == null || workerId.isBlank() ? DEFAULT_WORKER_ID : workerId.trim(),
                    claimLimit <= 0 ? DEFAULT_CLAIM_LIMIT : claimLimit);
            return new BiQuickEngineQueueSchedulerResult(
                    recovery.expired() + (claim == null ? 0 : claim.expired()),
                    recovery.recovered(),
                    claim == null ? 0 : claim.claimed(),
                    0);
        } finally {
            running.set(false);
        }
    }

    private BiQuickEngineQueueSchedulerResult empty() {
        return new BiQuickEngineQueueSchedulerResult(0, 0, 0, 0);
    }

    private String leaseKey(String poolKey) {
        return LEASE_KEY_PREFIX + normalizePoolKey(poolKey);
    }

    private String normalizePoolKey(String poolKey) {
        if (poolKey == null || poolKey.isBlank()) {
            return DEFAULT_POOL_KEY;
        }
        return poolKey.trim().toUpperCase(Locale.ROOT);
    }
}
