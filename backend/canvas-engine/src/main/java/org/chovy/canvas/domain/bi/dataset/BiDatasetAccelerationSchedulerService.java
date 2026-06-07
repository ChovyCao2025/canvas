package org.chovy.canvas.domain.bi.dataset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiDatasetAccelerationPolicyDO;
import org.chovy.canvas.dal.mapper.BiDatasetAccelerationPolicyMapper;
import org.chovy.canvas.domain.bi.query.BiQueryCacheInvalidationCommand;
import org.chovy.canvas.domain.bi.query.BiQueryCachePolicyService;
import org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerLeaseService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
public class BiDatasetAccelerationSchedulerService {

    private static final String SCHEDULER_LEASE_KEY = "BI_DATASET_ACCELERATION_SCHEDULER";
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final BiDatasetAccelerationPolicyMapper policyMapper;
    private final BiDatasetAccelerationService accelerationService;
    private final BiQueryCachePolicyService cachePolicyService;
    private final boolean enabled;
    private final Long tenantId;
    private final String operator;
    private final int limit;
    private final BiDeliverySchedulerLeaseService leaseService;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public BiDatasetAccelerationSchedulerService(BiDatasetAccelerationPolicyMapper policyMapper,
                                                 BiDatasetAccelerationService accelerationService,
                                                 BiQueryCachePolicyService cachePolicyService) {
        this(policyMapper,
                accelerationService,
                cachePolicyService,
                false,
                0L,
                "bi-dataset-acceleration-scheduler",
                500,
                (BiDeliverySchedulerLeaseService) null,
                120);
    }

    @Autowired
    public BiDatasetAccelerationSchedulerService(
            BiDatasetAccelerationPolicyMapper policyMapper,
            BiDatasetAccelerationService accelerationService,
            BiQueryCachePolicyService cachePolicyService,
            @Value("${canvas.bi.dataset.acceleration.scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.bi.dataset.acceleration.scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.bi.dataset.acceleration.scheduler.operator:bi-dataset-acceleration-scheduler}") String operator,
            @Value("${canvas.bi.dataset.acceleration.scheduler.limit:500}") int limit,
            ObjectProvider<BiDeliverySchedulerLeaseService> leaseServiceProvider,
            @Value("${canvas.bi.dataset.acceleration.scheduler.lease-ttl-seconds:120}") long leaseTtlSeconds) {
        this(policyMapper,
                accelerationService,
                cachePolicyService,
                enabled,
                tenantId,
                operator,
                limit,
                leaseServiceProvider == null ? null : leaseServiceProvider.getIfAvailable(),
                leaseTtlSeconds);
    }

    public BiDatasetAccelerationSchedulerService(BiDatasetAccelerationPolicyMapper policyMapper,
                                                 BiDatasetAccelerationService accelerationService,
                                                 BiQueryCachePolicyService cachePolicyService,
                                                 boolean enabled,
                                                 Long tenantId,
                                                 String operator,
                                                 int limit,
                                                 BiDeliverySchedulerLeaseService leaseService,
                                                 long leaseTtlSeconds) {
        this.policyMapper = policyMapper;
        this.accelerationService = accelerationService;
        this.cachePolicyService = cachePolicyService;
        this.enabled = enabled;
        this.tenantId = normalizeTenant(tenantId);
        this.operator = defaultText(operator, "bi-dataset-acceleration-scheduler");
        this.limit = Math.max(1, Math.min(limit, 1000));
        this.leaseService = leaseService;
        this.leaseTtlSeconds = Math.max(1, leaseTtlSeconds);
    }

    @Scheduled(fixedDelayString = "${canvas.bi.dataset.acceleration.scheduler.fixed-delay-ms:60000}")
    public void scheduledCycle() {
        runScheduledOnce(LocalDateTime.now());
    }

    public BiDatasetAccelerationSchedulerResult runScheduledOnce(LocalDateTime now) {
        if (!enabled) {
            return empty();
        }
        if (leaseService == null) {
            return runDueOnce(tenantId, operator, now);
        }
        if (!leaseService.acquire(tenantId, SCHEDULER_LEASE_KEY, leaseTtl())) {
            return new BiDatasetAccelerationSchedulerResult(0, 0, 1, 0);
        }
        try {
            return runDueOnce(tenantId, operator, now);
        } finally {
            leaseService.release(tenantId, SCHEDULER_LEASE_KEY);
        }
    }

    public BiDatasetAccelerationSchedulerResult runDueOnce(Long tenantId, String actor, LocalDateTime now) {
        if (!running.compareAndSet(false, true)) {
            return new BiDatasetAccelerationSchedulerResult(0, 0, 1, 0);
        }
        try {
            Long scopedTenantId = normalizeTenant(tenantId);
            String scopedActor = defaultText(actor, operator);
            LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
            int checked = 0;
            int refreshed = 0;
            int skipped = 0;
            int failed = 0;
            List<BiDatasetAccelerationSchedulerItem> items = new ArrayList<>();
            for (BiDatasetAccelerationPolicyDO policy : policies(scopedTenantId)) {
                checked++;
                if (!isRefreshCandidate(policy) || !isDue(policy, effectiveNow)) {
                    skipped++;
                    items.add(skippedItem(policy, isRefreshCandidate(policy) ? "not due" : "not scheduled extract"));
                    continue;
                }
                try {
                    BiDatasetExtractRefreshRunView run =
                            accelerationService.refreshNow(scopedTenantId, policy.getDatasetKey(), scopedActor);
                    if (run != null && STATUS_SUCCESS.equals(normalizeStatus(run.status()))) {
                        refreshed++;
                        items.add(runItem(policy.getDatasetKey(), "REFRESHED", run, normalizeStatus(run.status())));
                        invalidateDatasetCache(policy.getDatasetKey());
                    } else {
                        skipped++;
                        items.add(runItem(
                                policy.getDatasetKey(),
                                "SKIPPED",
                                run,
                                run == null ? "refresh returned no run" : "refresh returned " + normalizeStatus(run.status())));
                    }
                } catch (RuntimeException e) {
                    failed++;
                    items.add(new BiDatasetAccelerationSchedulerItem(
                            policy.getDatasetKey(),
                            "FAILED",
                            errorReason(e),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null));
                }
            }
            return new BiDatasetAccelerationSchedulerResult(checked, refreshed, skipped, failed, items);
        } finally {
            running.set(false);
        }
    }

    private List<BiDatasetAccelerationPolicyDO> policies(Long tenantId) {
        List<BiDatasetAccelerationPolicyDO> rows = policyMapper.selectList(
                new LambdaQueryWrapper<BiDatasetAccelerationPolicyDO>()
                        .eq(BiDatasetAccelerationPolicyDO::getTenantId, tenantId)
                        .eq(BiDatasetAccelerationPolicyDO::getEnabled, true)
                        .eq(BiDatasetAccelerationPolicyDO::getAccelerationMode, BiDatasetAccelerationService.MODE_EXTRACT)
                        .eq(BiDatasetAccelerationPolicyDO::getRefreshMode, BiDatasetAccelerationService.REFRESH_SCHEDULED)
                        .orderByAsc(BiDatasetAccelerationPolicyDO::getId)
                        .last("LIMIT " + limit));
        return rows == null ? List.of() : rows;
    }

    private boolean isRefreshCandidate(BiDatasetAccelerationPolicyDO policy) {
        return policy != null
                && Boolean.TRUE.equals(policy.getEnabled())
                && hasText(policy.getDatasetKey())
                && BiDatasetAccelerationService.MODE_EXTRACT.equals(normalize(policy.getAccelerationMode()))
                && BiDatasetAccelerationService.REFRESH_SCHEDULED.equals(normalize(policy.getRefreshMode()));
    }

    private boolean isDue(BiDatasetAccelerationPolicyDO policy, LocalDateTime now) {
        if (policy.getLastRefreshedAt() == null) {
            return true;
        }
        if (hasText(policy.getCronExpression())) {
            return isCronDue(policy.getCronExpression(), policy.getLastRefreshedAt(), now);
        }
        return !policy.getLastRefreshedAt().plusMinutes(intervalMinutes(policy)).isAfter(now);
    }

    private boolean isCronDue(String cronExpression, LocalDateTime lastRefreshedAt, LocalDateTime now) {
        try {
            CronExpression parsed = CronExpression.parse(cronExpression.trim());
            LocalDateTime next = parsed.next(lastRefreshedAt);
            return next != null && !next.isAfter(now);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private long intervalMinutes(BiDatasetAccelerationPolicyDO policy) {
        Long value = policy.getRefreshIntervalMinutes();
        return value == null || value <= 0 ? 60L : value;
    }

    private void invalidateDatasetCache(String datasetKey) {
        if (cachePolicyService != null) {
            cachePolicyService.invalidate(new BiQueryCacheInvalidationCommand("DATASET", null, datasetKey));
        }
    }

    private BiDatasetAccelerationSchedulerItem skippedItem(BiDatasetAccelerationPolicyDO policy, String reason) {
        return new BiDatasetAccelerationSchedulerItem(
                policy == null ? null : policy.getDatasetKey(),
                "SKIPPED",
                reason,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private BiDatasetAccelerationSchedulerItem runItem(String datasetKey,
                                                       String status,
                                                       BiDatasetExtractRefreshRunView run,
                                                       String reason) {
        return new BiDatasetAccelerationSchedulerItem(
                datasetKey,
                status,
                defaultText(reason, "-"),
                run == null ? null : run.id(),
                run == null ? null : run.rowCount(),
                run == null ? null : run.durationMs(),
                run == null ? null : run.materializedTable(),
                run == null ? null : run.startedAt(),
                run == null ? null : run.finishedAt());
    }

    private String errorReason(RuntimeException e) {
        if (e == null || !hasText(e.getMessage())) {
            return "refresh failed";
        }
        return e.getMessage().trim();
    }

    private BiDatasetAccelerationSchedulerResult empty() {
        return new BiDatasetAccelerationSchedulerResult(0, 0, 0, 0);
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(leaseTtlSeconds);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String normalize(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String normalizeStatus(String status) {
        return normalize(status);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
