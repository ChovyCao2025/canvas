package org.chovy.canvas.domain.warehouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CdpWarehouseRetentionScheduler 编排 domain.warehouse 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class CdpWarehouseRetentionScheduler {

    private static final String OPERATOR = "warehouse-retention-scheduler";
    private static final String LEASE_KEY = "CDP_WAREHOUSE_RETENTION";

    private final CdpWarehouseRetentionService retentionService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int syncRunRetentionDays;
    private final int realtimeRetryRetentionDays;
    private final int resolvedIncidentRetentionDays;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 CdpWarehouseRetentionScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param retentionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseRetentionScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param syncRunRetentionDays sync run retention days 参数，用于 CdpWarehouseRetentionScheduler 流程中的校验、计算或对象转换。
     * @param realtimeRetryRetentionDays 时间参数，用于计算窗口、过期或审计时间。
     * @param resolvedIncidentRetentionDays resolved incident retention days 参数，用于 CdpWarehouseRetentionScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseRetentionScheduler(
            CdpWarehouseRetentionService retentionService,
            @Value("${canvas.warehouse.retention.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.retention.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.retention.sync-run-retention-days:30}") int syncRunRetentionDays,
            @Value("${canvas.warehouse.retention.realtime-retry-retention-days:14}") int realtimeRetryRetentionDays,
            @Value("${canvas.warehouse.retention.resolved-incident-retention-days:90}") int resolvedIncidentRetentionDays) {
        this(retentionService, null, enabled, tenantId, syncRunRetentionDays, realtimeRetryRetentionDays,
                resolvedIncidentRetentionDays, 300);
    }

    /**
     * 创建 CdpWarehouseRetentionScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param retentionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseRetentionScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param syncRunRetentionDays sync run retention days 参数，用于 CdpWarehouseRetentionScheduler 流程中的校验、计算或对象转换。
     * @param realtimeRetryRetentionDays 时间参数，用于计算窗口、过期或审计时间。
     * @param resolvedIncidentRetentionDays resolved incident retention days 参数，用于 CdpWarehouseRetentionScheduler 流程中的校验、计算或对象转换。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseRetentionScheduler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public CdpWarehouseRetentionScheduler(
            CdpWarehouseRetentionService retentionService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.retention.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.retention.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.retention.sync-run-retention-days:30}") int syncRunRetentionDays,
            @Value("${canvas.warehouse.retention.realtime-retry-retention-days:14}") int realtimeRetryRetentionDays,
            @Value("${canvas.warehouse.retention.resolved-incident-retention-days:90}") int resolvedIncidentRetentionDays,
            @Value("${canvas.warehouse.retention.lease-ttl-seconds:300}") long leaseTtlSeconds) {
        this.retentionService = retentionService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.syncRunRetentionDays = syncRunRetentionDays;
        this.realtimeRetryRetentionDays = realtimeRetryRetentionDays;
        this.resolvedIncidentRetentionDays = resolvedIncidentRetentionDays;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    /**
     * CDP 仓库保留期清理任务的 Spring 调度入口。
     *
     * <p>该入口按日级默认周期触发并传入当前时间；实际业务由 {@link #runCycle(LocalDateTime)} 委托 retentionService
     * 清理过期同步运行、实时重试和已解决事件记录。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.warehouse.retention.fixed-delay-ms:86400000}")
    public void scheduledCycle() {
        runCycle(LocalDateTime.now());
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    boolean runCycle(LocalDateTime now) {
        if (!enabled) {
            return false;
        }
        if (leaseService != null) {
            return leaseService.runWithLease(tenantId, LEASE_KEY, leaseTtl(), () -> executeCycle(now));
        }
        return executeCycle(now);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    private boolean executeCycle(LocalDateTime now) {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        try {
            retentionService.cleanup(
                    tenantId,
                    now,
                    syncRunRetentionDays,
                    realtimeRetryRetentionDays,
                    resolvedIncidentRetentionDays,
                    OPERATOR);
            return true;
        } finally {
            running.set(false);
        }
    }

    /**
     * 执行 leaseTtl 流程，围绕 lease ttl 完成校验、计算或结果组装。
     *
     * @return 返回 leaseTtl 流程生成的业务结果。
     */
    private Duration leaseTtl() {
        return Duration.ofSeconds(Math.max(leaseTtlSeconds, 1));
    }
}
