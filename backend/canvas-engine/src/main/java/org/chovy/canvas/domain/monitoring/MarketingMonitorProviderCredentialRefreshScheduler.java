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
 * MarketingMonitorProviderCredentialRefreshScheduler 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class MarketingMonitorProviderCredentialRefreshScheduler {

    static final String LEASE_KEY = "MARKETING_MONITOR_PROVIDER_CREDENTIAL_REFRESH";

    private final MarketingMonitorProviderCredentialService credentialService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int windowMinutes;
    private final int limit;
    private final String operator;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 MarketingMonitorProviderCredentialRefreshScheduler 实例并注入 domain.monitoring 场景依赖。
     * @param credentialService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 MarketingMonitorProviderCredentialRefreshScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param windowMinutes window minutes 参数，用于 MarketingMonitorProviderCredentialRefreshScheduler 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     */
    public MarketingMonitorProviderCredentialRefreshScheduler(
            MarketingMonitorProviderCredentialService credentialService,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.window-minutes:30}") int windowMinutes,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.limit:50}") int limit,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.operator:monitoring-token-scheduler}") String operator) {
        this(credentialService, null, enabled, tenantId, windowMinutes, limit, operator, 120);
    }

    /**
     * 创建 MarketingMonitorProviderCredentialRefreshScheduler 实例并注入 domain.monitoring 场景依赖。
     * @param credentialService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 MarketingMonitorProviderCredentialRefreshScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param windowMinutes window minutes 参数，用于 MarketingMonitorProviderCredentialRefreshScheduler 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 MarketingMonitorProviderCredentialRefreshScheduler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public MarketingMonitorProviderCredentialRefreshScheduler(
            MarketingMonitorProviderCredentialService credentialService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.window-minutes:30}") int windowMinutes,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.limit:50}") int limit,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.operator:monitoring-token-scheduler}") String operator,
            @Value("${canvas.monitoring.provider-credential-refresh-scheduler.lease-ttl-seconds:120}") long leaseTtlSeconds) {
        this.credentialService = credentialService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.windowMinutes = windowMinutes;
        this.limit = limit;
        this.operator = operator == null || operator.isBlank() ? "monitoring-token-scheduler" : operator.trim();
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    /**
     * 监控提供方凭据刷新任务的 Spring 调度入口。
     *
     * <p>该入口按固定延迟传入当前时间并委托 {@link #runCycle(LocalDateTime)}；实际业务是在租约和并发保护下
     * 查找即将过期的托管凭据并调用刷新流程。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.monitoring.provider-credential-refresh-scheduler.fixed-delay-ms:60000}")
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
            credentialService.refreshDue(tenantId,
                    new MarketingMonitorProviderCredentialDueRefreshCommand(windowMinutes, limit),
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
