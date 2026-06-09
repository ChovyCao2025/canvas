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
 * CdpWarehouseAvailabilityIncidentScheduler 编排 domain.warehouse 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class CdpWarehouseAvailabilityIncidentScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_AVAILABILITY_INCIDENT";

    private final CdpWarehouseAvailabilityIncidentService incidentService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final String mode;
    private final long windowMinutes;
    private final String operator;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 CdpWarehouseAvailabilityIncidentScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseAvailabilityIncidentScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     */
    public CdpWarehouseAvailabilityIncidentScheduler(
            CdpWarehouseAvailabilityIncidentService incidentService,
            @Value("${canvas.warehouse.availability-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.availability-incident-scheduler.tenant-id:0}") Long tenantId) {
        this(incidentService, null, enabled, tenantId, "HYBRID", 60, "availability-incident-scheduler", 60);
    }

    /**
     * 创建 CdpWarehouseAvailabilityIncidentScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseAvailabilityIncidentScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param mode mode 参数，用于 CdpWarehouseAvailabilityIncidentScheduler 流程中的校验、计算或对象转换。
     * @param windowMinutes window minutes 参数，用于 CdpWarehouseAvailabilityIncidentScheduler 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseAvailabilityIncidentScheduler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public CdpWarehouseAvailabilityIncidentScheduler(
            CdpWarehouseAvailabilityIncidentService incidentService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.availability-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.availability-incident-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.availability-incident-scheduler.mode:HYBRID}") String mode,
            @Value("${canvas.warehouse.availability-incident-scheduler.window-minutes:60}") long windowMinutes,
            @Value("${canvas.warehouse.availability-incident-scheduler.operator:availability-incident-scheduler}") String operator,
            @Value("${canvas.warehouse.availability-incident-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.incidentService = incidentService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.mode = mode == null || mode.isBlank() ? "HYBRID" : mode.trim();
        this.windowMinutes = windowMinutes;
        this.operator = operator == null || operator.isBlank() ? "availability-incident-scheduler" : operator.trim();
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    /**
     * CDP 仓库可用性事件扫描的 Spring 调度入口。
     *
     * <p>该入口只提供调度触发和当前时间；实际业务由 {@link #runCycle(LocalDateTime)} 在可选租约保护下委托
     * incidentService 扫描指定窗口内的仓库可用性告警。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.warehouse.availability-incident-scheduler.fixed-delay-ms:60000}")
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
            LocalDateTime evaluatedAt = now == null ? LocalDateTime.now() : now;
            incidentService.scan(
                    tenantId,
                    evaluatedAt.minusMinutes(Math.max(windowMinutes, 1)),
                    evaluatedAt,
                    mode,
                    operator);
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
