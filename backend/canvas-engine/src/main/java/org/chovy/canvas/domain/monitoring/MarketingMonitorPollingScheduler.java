package org.chovy.canvas.domain.monitoring;

import org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MarketingMonitorPollingScheduler 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class MarketingMonitorPollingScheduler {

    private static final String LEASE_KEY = "MARKETING_MONITOR_POLLING";

    private final MarketingMonitorPollingScheduleService scheduleService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int limit;
    private final String operator;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 MarketingMonitorPollingScheduler 实例并注入 domain.monitoring 场景依赖。
     * @param scheduleService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 MarketingMonitorPollingScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     */
    public MarketingMonitorPollingScheduler(
            MarketingMonitorPollingScheduleService scheduleService,
            @Value("${canvas.monitoring.polling-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.monitoring.polling-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.monitoring.polling-scheduler.limit:50}") int limit,
            @Value("${canvas.monitoring.polling-scheduler.operator:monitoring-scheduler}") String operator) {
        this(scheduleService, null, enabled, tenantId, limit, operator, 120);
    }

    /**
     * 创建 MarketingMonitorPollingScheduler 实例并注入 domain.monitoring 场景依赖。
     * @param scheduleService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 MarketingMonitorPollingScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 MarketingMonitorPollingScheduler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public MarketingMonitorPollingScheduler(
            MarketingMonitorPollingScheduleService scheduleService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.monitoring.polling-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.monitoring.polling-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.monitoring.polling-scheduler.limit:50}") int limit,
            @Value("${canvas.monitoring.polling-scheduler.operator:monitoring-scheduler}") String operator,
            @Value("${canvas.monitoring.polling-scheduler.lease-ttl-seconds:120}") long leaseTtlSeconds) {
        this.scheduleService = scheduleService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.limit = limit;
        this.operator = operator;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    /**
     * 营销监控源轮询的 Spring 调度入口。
     *
     * <p>该入口按固定延迟提供当前时间并委托 {@link #runCycle(LocalDateTime)}；实际业务是在可选租约和并发保护下
     * 扫描到期监控源，拉取外部内容并推进轮询游标。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.monitoring.polling-scheduler.fixed-delay-ms:60000}")
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
            scheduleService.pollDueSources(tenantId, now, limit, operator);
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
