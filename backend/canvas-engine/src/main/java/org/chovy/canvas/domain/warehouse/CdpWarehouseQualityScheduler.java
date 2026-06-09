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
 * CdpWarehouseQualityScheduler 编排 domain.warehouse 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class CdpWarehouseQualityScheduler {

    private static final String OPERATOR = "warehouse-quality-scheduler";
    private static final String LEASE_KEY = "CDP_WAREHOUSE_QUALITY";

    private final CdpWarehouseQualityService qualityService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int windowMinutes;
    private final long tolerance;
    private final long maxLagMinutes;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 CdpWarehouseQualityScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param qualityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseQualityScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param windowMinutes window minutes 参数，用于 CdpWarehouseQualityScheduler 流程中的校验、计算或对象转换。
     * @param tolerance tolerance 参数，用于 CdpWarehouseQualityScheduler 流程中的校验、计算或对象转换。
     * @param maxLagMinutes max lag minutes 参数，用于 CdpWarehouseQualityScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseQualityScheduler(
            CdpWarehouseQualityService qualityService,
            @Value("${canvas.warehouse.quality.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.quality.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.quality.window-minutes:60}") int windowMinutes,
            @Value("${canvas.warehouse.quality.ods-count-tolerance:0}") long tolerance,
            @Value("${canvas.warehouse.quality.max-aggregate-lag-minutes:30}") long maxLagMinutes) {
        this(qualityService, null, enabled, tenantId, windowMinutes, tolerance, maxLagMinutes, 600);
    }

    /**
     * 创建 CdpWarehouseQualityScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param qualityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseQualityScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param windowMinutes window minutes 参数，用于 CdpWarehouseQualityScheduler 流程中的校验、计算或对象转换。
     * @param tolerance tolerance 参数，用于 CdpWarehouseQualityScheduler 流程中的校验、计算或对象转换。
     * @param maxLagMinutes max lag minutes 参数，用于 CdpWarehouseQualityScheduler 流程中的校验、计算或对象转换。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseQualityScheduler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public CdpWarehouseQualityScheduler(
            CdpWarehouseQualityService qualityService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.quality.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.quality.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.quality.window-minutes:60}") int windowMinutes,
            @Value("${canvas.warehouse.quality.ods-count-tolerance:0}") long tolerance,
            @Value("${canvas.warehouse.quality.max-aggregate-lag-minutes:30}") long maxLagMinutes,
            @Value("${canvas.warehouse.quality.lease-ttl-seconds:600}") long leaseTtlSeconds) {
        this.qualityService = qualityService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.windowMinutes = windowMinutes;
        this.tolerance = tolerance;
        this.maxLagMinutes = maxLagMinutes;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    /**
     * CDP 仓库质量巡检任务的 Spring 调度入口。
     *
     * <p>该入口按固定延迟传入当前时间；实际业务由 {@link #runCycle(LocalDateTime)} 委托 qualityService
     * 进行 ODS 数量对账和聚合延迟检查。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.warehouse.quality.fixed-delay-ms:300000}")
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
            LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
            LocalDateTime from = effectiveNow.minusMinutes(Math.max(windowMinutes, 1));
            qualityService.reconcileOds(tenantId, from, effectiveNow, tolerance, OPERATOR);
            qualityService.checkAggregateLag(tenantId, effectiveNow, maxLagMinutes, OPERATOR);
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
