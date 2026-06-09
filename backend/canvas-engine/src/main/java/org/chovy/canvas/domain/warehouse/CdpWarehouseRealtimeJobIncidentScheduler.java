package org.chovy.canvas.domain.warehouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CdpWarehouseRealtimeJobIncidentScheduler 编排 domain.warehouse 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class CdpWarehouseRealtimeJobIncidentScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_REALTIME_JOB_INCIDENT";

    private final CdpWarehouseRealtimeJobIncidentService incidentService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final String pipelineKey;
    private final long maxHeartbeatAgeSeconds;
    private final int limit;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 CdpWarehouseRealtimeJobIncidentScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseRealtimeJobIncidentScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     */
    public CdpWarehouseRealtimeJobIncidentScheduler(
            CdpWarehouseRealtimeJobIncidentService incidentService,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.tenant-id:0}") Long tenantId) {
        this(incidentService, null, enabled, tenantId, null, 300, 50, 60);
    }

    /**
     * 创建 CdpWarehouseRealtimeJobIncidentScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseRealtimeJobIncidentScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param maxHeartbeatAgeSeconds max heartbeat age seconds 参数，用于 CdpWarehouseRealtimeJobIncidentScheduler 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseRealtimeJobIncidentScheduler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public CdpWarehouseRealtimeJobIncidentScheduler(
            CdpWarehouseRealtimeJobIncidentService incidentService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.pipeline-key:}") String pipelineKey,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.max-heartbeat-age-seconds:300}") long maxHeartbeatAgeSeconds,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.limit:50}") int limit,
            @Value("${canvas.warehouse.realtime-job-incident-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.incidentService = incidentService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.pipelineKey = hasText(pipelineKey) ? pipelineKey.trim() : null;
        this.maxHeartbeatAgeSeconds = maxHeartbeatAgeSeconds;
        this.limit = limit;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    /**
     * 实时作业心跳异常事件扫描的 Spring 调度入口。
     *
     * <p>该入口只触发一次调度周期；实际业务由 {@link #runCycle()} 在租约和本地并发保护下委托
     * incidentService 按租户、流水线和心跳阈值扫描实时作业异常。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.warehouse.realtime-job-incident-scheduler.fixed-delay-ms:60000}")
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
            incidentService.scan(tenantId, pipelineKey, maxHeartbeatAgeSeconds, limit);
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

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
