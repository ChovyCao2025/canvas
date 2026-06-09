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
 * CdpWarehouseScheduler 编排 domain.warehouse 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class CdpWarehouseScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_MAIN";

    private final CdpWarehouseOperationsService operationsService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int backfillLimit;
    private final int aggregationWindowMinutes;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 CdpWarehouseScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param backfillLimit backfill limit 参数，用于 CdpWarehouseScheduler 流程中的校验、计算或对象转换。
     * @param aggregationWindowMinutes aggregation window minutes 参数，用于 CdpWarehouseScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseScheduler(
            CdpWarehouseOperationsService operationsService,
            @Value("${canvas.warehouse.scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.scheduler.backfill-limit:1000}") int backfillLimit,
            @Value("${canvas.warehouse.scheduler.aggregation-window-minutes:30}") int aggregationWindowMinutes) {
        this(operationsService, null, enabled, tenantId, backfillLimit, aggregationWindowMinutes, 120);
    }

    /**
     * 创建 CdpWarehouseScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param backfillLimit backfill limit 参数，用于 CdpWarehouseScheduler 流程中的校验、计算或对象转换。
     * @param aggregationWindowMinutes aggregation window minutes 参数，用于 CdpWarehouseScheduler 流程中的校验、计算或对象转换。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseScheduler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public CdpWarehouseScheduler(
            CdpWarehouseOperationsService operationsService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.scheduler.backfill-limit:1000}") int backfillLimit,
            @Value("${canvas.warehouse.scheduler.aggregation-window-minutes:30}") int aggregationWindowMinutes,
            @Value("${canvas.warehouse.scheduler.lease-ttl-seconds:120}") long leaseTtlSeconds) {
        this.operationsService = operationsService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.backfillLimit = backfillLimit;
        this.aggregationWindowMinutes = aggregationWindowMinutes;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    /**
     * CDP 仓库离线主循环的 Spring 调度入口。
     *
     * <p>该入口只注入当前时间并委托 {@link #runCycle(LocalDateTime)}；实际业务是在租约保护下运行离线补数和聚合窗口处理，
     * 由 operationsService 负责落库和统计更新。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.warehouse.scheduler.fixed-delay-ms:60000}")
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
            operationsService.runScheduledOfflineCycle(tenantId, now, backfillLimit, aggregationWindowMinutes);
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
