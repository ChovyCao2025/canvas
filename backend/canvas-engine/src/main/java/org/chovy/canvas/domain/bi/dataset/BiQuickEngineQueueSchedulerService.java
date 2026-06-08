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
/**
 * BiQuickEngineQueueSchedulerService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 BiQuickEngineQueueSchedulerService 实例。
     *
     * @param queueService 依赖组件，用于完成数据访问或外部能力调用。
     */
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
    /**
     * 初始化 BiQuickEngineQueueSchedulerService 实例。
     *
     * @param queueService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 BiQuickEngineQueueSchedulerService 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param staleClaimSeconds stale claim seconds 参数，用于 BiQuickEngineQueueSchedulerService 流程中的校验、计算或对象转换。
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @param claimLimit claim limit 参数，用于 BiQuickEngineQueueSchedulerService 流程中的校验、计算或对象转换。
     * @param leaseServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 BiQuickEngineQueueSchedulerService 流程中的校验、计算或对象转换。
     */
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

    /**
     * 初始化 BiQuickEngineQueueSchedulerService 实例。
     *
     * @param queueService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 BiQuickEngineQueueSchedulerService 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param staleClaimSeconds stale claim seconds 参数，用于 BiQuickEngineQueueSchedulerService 流程中的校验、计算或对象转换。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 BiQuickEngineQueueSchedulerService 流程中的校验、计算或对象转换。
     */
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

    /**
     * 初始化 BiQuickEngineQueueSchedulerService 实例。
     *
     * @param queueService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 BiQuickEngineQueueSchedulerService 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param staleClaimSeconds stale claim seconds 参数，用于 BiQuickEngineQueueSchedulerService 流程中的校验、计算或对象转换。
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @param claimLimit claim limit 参数，用于 BiQuickEngineQueueSchedulerService 流程中的校验、计算或对象转换。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 BiQuickEngineQueueSchedulerService 流程中的校验、计算或对象转换。
     */
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
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     */
    public void scheduledCycle() {
        runScheduledOnce();
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @return 返回流程执行后的业务结果。
     */
    public BiQuickEngineQueueSchedulerResult runScheduledOnce() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return runMaintenanceOnce(tenantId, poolKey, staleClaimSeconds, workerId, claimLimit);
        } finally {
            leaseService.release(tenantId, leaseKey);
        }
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param staleClaimSeconds stale claim seconds 参数，用于 runRecoveryOnce 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param staleClaimSeconds stale claim seconds 参数，用于 runMaintenanceOnce 流程中的校验、计算或对象转换。
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @param claimLimit claim limit 参数，用于 runMaintenanceOnce 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
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
                    0,
                    claim == null ? null : claim.jobs());
        } finally {
            running.set(false);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 empty 流程生成的业务结果。
     */
    private BiQuickEngineQueueSchedulerResult empty() {
        return new BiQuickEngineQueueSchedulerResult(0, 0, 0, 0);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @return 返回 lease key 生成的文本或业务键。
     */
    private String leaseKey(String poolKey) {
        return LEASE_KEY_PREFIX + normalizePoolKey(poolKey);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizePoolKey(String poolKey) {
        if (poolKey == null || poolKey.isBlank()) {
            return DEFAULT_POOL_KEY;
        }
        return poolKey.trim().toUpperCase(Locale.ROOT);
    }
}
