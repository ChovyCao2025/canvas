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
/**
 * BiDatasetAccelerationSchedulerService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 BiDatasetAccelerationSchedulerService 实例。
     *
     * @param policyMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param accelerationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param cachePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     */
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
    /**
     * 初始化 BiDatasetAccelerationSchedulerService 实例。
     *
     * @param policyMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param accelerationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param cachePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 BiDatasetAccelerationSchedulerService 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param leaseServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 BiDatasetAccelerationSchedulerService 流程中的校验、计算或对象转换。
     */
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

    /**
     * 初始化 BiDatasetAccelerationSchedulerService 实例。
     *
     * @param policyMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param accelerationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param cachePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 BiDatasetAccelerationSchedulerService 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 BiDatasetAccelerationSchedulerService 流程中的校验、计算或对象转换。
     */
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
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     */
    public void scheduledCycle() {
        runScheduledOnce(LocalDateTime.now());
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    public BiDatasetAccelerationSchedulerResult runScheduledOnce(LocalDateTime now) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return runDueOnce(tenantId, operator, now);
        } finally {
            leaseService.release(tenantId, SCHEDULER_LEASE_KEY);
        }
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    public BiDatasetAccelerationSchedulerResult runDueOnce(Long tenantId, String actor, LocalDateTime now) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new BiDatasetAccelerationSchedulerResult(checked, refreshed, skipped, failed, items);
        } finally {
            running.set(false);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 policies 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param policy policy 参数，用于 isRefreshCandidate 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isRefreshCandidate(BiDatasetAccelerationPolicyDO policy) {
        return policy != null
                && Boolean.TRUE.equals(policy.getEnabled())
                && hasText(policy.getDatasetKey())
                && BiDatasetAccelerationService.MODE_EXTRACT.equals(normalize(policy.getAccelerationMode()))
                && BiDatasetAccelerationService.REFRESH_SCHEDULED.equals(normalize(policy.getRefreshMode()));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param policy policy 参数，用于 isDue 流程中的校验、计算或对象转换。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
    private boolean isDue(BiDatasetAccelerationPolicyDO policy, LocalDateTime now) {
        if (policy.getLastRefreshedAt() == null) {
            return true;
        }
        if (hasText(policy.getCronExpression())) {
            return isCronDue(policy.getCronExpression(), policy.getLastRefreshedAt(), now);
        }
        return !policy.getLastRefreshedAt().plusMinutes(intervalMinutes(policy)).isAfter(now);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param cronExpression cron expression 参数，用于 isCronDue 流程中的校验、计算或对象转换。
     * @param lastRefreshedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
    private boolean isCronDue(String cronExpression, LocalDateTime lastRefreshedAt, LocalDateTime now) {
        try {
            CronExpression parsed = CronExpression.parse(cronExpression.trim());
            LocalDateTime next = parsed.next(lastRefreshedAt);
            return next != null && !next.isAfter(now);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param policy policy 参数，用于 intervalMinutes 流程中的校验、计算或对象转换。
     * @return 返回 interval minutes 计算得到的数量、金额或指标值。
     */
    private long intervalMinutes(BiDatasetAccelerationPolicyDO policy) {
        Long value = policy.getRefreshIntervalMinutes();
        return value == null || value <= 0 ? 60L : value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     */
    private void invalidateDatasetCache(String datasetKey) {
        if (cachePolicyService != null) {
            cachePolicyService.invalidate(new BiQueryCacheInvalidationCommand("DATASET", null, datasetKey));
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param policy policy 参数，用于 skippedItem 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回 skippedItem 流程生成的业务结果。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param run run 参数，用于 runItem 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回流程执行后的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param e e 参数，用于 errorReason 流程中的校验、计算或对象转换。
     * @return 返回 error reason 生成的文本或业务键。
     */
    private String errorReason(RuntimeException e) {
        if (e == null || !hasText(e.getMessage())) {
            return "refresh failed";
        }
        return e.getMessage().trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 empty 流程生成的业务结果。
     */
    private BiDatasetAccelerationSchedulerResult empty() {
        return new BiDatasetAccelerationSchedulerResult(0, 0, 0, 0);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 leaseTtl 流程生成的业务结果。
     */
    private Duration leaseTtl() {
        return Duration.ofSeconds(leaseTtlSeconds);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultText 流程中的校验、计算或对象转换。
     * @return 返回 default text 生成的文本或业务键。
     */
    private String defaultText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalize(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        return normalize(status);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
