package org.chovy.canvas.domain.warehouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CdpWarehouseTableDriftIncidentScheduler 编排 domain.warehouse 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class CdpWarehouseTableDriftIncidentScheduler {

    private static final String OPERATOR = "warehouse-table-drift-scheduler";
    private static final String LEASE_KEY = "CDP_WAREHOUSE_TABLE_DRIFT_INCIDENT";

    private final CdpWarehouseTableDriftIncidentService incidentService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final boolean live;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 CdpWarehouseTableDriftIncidentScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseTableDriftIncidentScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     */
    public CdpWarehouseTableDriftIncidentScheduler(
            CdpWarehouseTableDriftIncidentService incidentService,
            @Value("${canvas.warehouse.table-drift-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.table-drift-incident-scheduler.tenant-id:0}") Long tenantId) {
        this(incidentService, null, enabled, tenantId, true, 300);
    }

    /**
     * 创建 CdpWarehouseTableDriftIncidentScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseTableDriftIncidentScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param live live 参数，用于 CdpWarehouseTableDriftIncidentScheduler 流程中的校验、计算或对象转换。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseTableDriftIncidentScheduler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public CdpWarehouseTableDriftIncidentScheduler(
            CdpWarehouseTableDriftIncidentService incidentService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.table-drift-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.table-drift-incident-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.table-drift-incident-scheduler.live:true}") boolean live,
            @Value("${canvas.warehouse.table-drift-incident-scheduler.lease-ttl-seconds:300}") long leaseTtlSeconds) {
        this.incidentService = incidentService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.live = live;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    /**
     * CDP 仓库表结构漂移事件扫描的 Spring 调度入口。
     *
     * <p>该入口按固定延迟触发；实际业务由 {@link #runCycle()} 委托 incidentService 按租户扫描线上或快照表结构差异，
     * 并以调度器身份记录事件。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.warehouse.table-drift-incident-scheduler.fixed-delay-ms:300000}")
    public void scheduledCycle() {
        runCycle();
    }

    /**
     * 执行核心业务处理流程。
     *
     * @return 返回流程执行后的业务结果。
     */
    boolean runCycle() {
        if (!enabled) {
            return false;
        }
        if (leaseService != null) {
            return leaseService.runWithLease(tenantId, LEASE_KEY, leaseTtl(), this::executeCycle);
        }
        return executeCycle();
    }

    /**
     * 执行核心业务处理流程。
     *
     * @return 返回流程执行后的业务结果。
     */
    private boolean executeCycle() {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        try {
            incidentService.scan(tenantId, live, OPERATOR);
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
