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
 * CdpWarehouseRealtimeRetryScheduler 编排 domain.warehouse 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class CdpWarehouseRealtimeRetryScheduler {

    private static final String LEASE_KEY = "CDP_WAREHOUSE_REALTIME_RETRY";

    private final CdpWarehouseRealtimeRetryService retryService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int limit;
    private final int maxAttempts;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 CdpWarehouseRealtimeRetryScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param retryService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseRealtimeRetryScheduler 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param maxAttempts max attempts 参数，用于 CdpWarehouseRealtimeRetryScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseRealtimeRetryScheduler(
            CdpWarehouseRealtimeRetryService retryService,
            @Value("${canvas.warehouse.realtime-retry.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.realtime-retry.limit:100}") int limit,
            @Value("${canvas.warehouse.realtime-retry.max-attempts:3}") int maxAttempts) {
        this(retryService, null, enabled, 0L, limit, maxAttempts, 60);
    }

    /**
     * 创建 CdpWarehouseRealtimeRetryScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param retryService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseRealtimeRetryScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param maxAttempts max attempts 参数，用于 CdpWarehouseRealtimeRetryScheduler 流程中的校验、计算或对象转换。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseRealtimeRetryScheduler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public CdpWarehouseRealtimeRetryScheduler(
            CdpWarehouseRealtimeRetryService retryService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.realtime-retry.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.realtime-retry.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.realtime-retry.limit:100}") int limit,
            @Value("${canvas.warehouse.realtime-retry.max-attempts:3}") int maxAttempts,
            @Value("${canvas.warehouse.realtime-retry.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.retryService = retryService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.limit = limit;
        this.maxAttempts = maxAttempts;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    /**
     * 实时链路重试任务的 Spring 调度入口。
     *
     * <p>该入口按固定延迟传入当前时间；实际业务由 {@link #runCycle(LocalDateTime)} 委托重试服务领取到期重试记录，
     * 并按配置的批量大小和最大尝试次数执行重放。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.warehouse.realtime-retry.fixed-delay-ms:30000}")
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
            retryService.retryDue(now, limit, maxAttempts);
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
