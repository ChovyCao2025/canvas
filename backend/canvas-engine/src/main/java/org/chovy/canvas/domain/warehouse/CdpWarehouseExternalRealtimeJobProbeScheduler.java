package org.chovy.canvas.domain.warehouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CdpWarehouseExternalRealtimeJobProbeScheduler 编排 domain.warehouse 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class CdpWarehouseExternalRealtimeJobProbeScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_EXTERNAL_REALTIME_JOB_PROBE";

    private final CdpWarehouseExternalRealtimeJobProbeService probeService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int limit;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 CdpWarehouseExternalRealtimeJobProbeScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param probeService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseExternalRealtimeJobProbeScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     */
    public CdpWarehouseExternalRealtimeJobProbeScheduler(
            CdpWarehouseExternalRealtimeJobProbeService probeService,
            @Value("${canvas.warehouse.external-realtime-job-probe-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.external-realtime-job-probe-scheduler.tenant-id:0}") Long tenantId) {
        this(probeService, null, enabled, tenantId, 50, 60);
    }

    /**
     * 创建 CdpWarehouseExternalRealtimeJobProbeScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param probeService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseExternalRealtimeJobProbeScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseExternalRealtimeJobProbeScheduler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public CdpWarehouseExternalRealtimeJobProbeScheduler(
            CdpWarehouseExternalRealtimeJobProbeService probeService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.external-realtime-job-probe-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.external-realtime-job-probe-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.external-realtime-job-probe-scheduler.limit:50}") int limit,
            @Value("${canvas.warehouse.external-realtime-job-probe-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.probeService = probeService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.limit = limit;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    /**
     * 外部实时作业探测任务的 Spring 调度入口。
     *
     * <p>该入口只触发调度周期；实际业务由 {@link #runCycle()} 在租约和本地并发保护下委托 probeService
     * 扫描待探测作业并调用外部运行时探针。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.warehouse.external-realtime-job-probe-scheduler.fixed-delay-ms:60000}")
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
            probeService.scan(tenantId, new CdpWarehouseExternalRealtimeJobProbeService.ScanCommand(null, limit));
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
