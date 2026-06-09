package org.chovy.canvas.domain.marketing;

import org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MarketingIntegrationContractProbeScheduler 编排 domain.marketing 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class MarketingIntegrationContractProbeScheduler {

    private static final String LEASE_KEY = "MARKETING_INTEGRATION_CONTRACT_PROBES";

    private final MarketingIntegrationContractProbeAutomationService automationService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int limit;
    private final String operator;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 MarketingIntegrationContractProbeScheduler 实例并注入 domain.marketing 场景依赖。
     * @param automationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 MarketingIntegrationContractProbeScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     */
    public MarketingIntegrationContractProbeScheduler(
            MarketingIntegrationContractProbeAutomationService automationService,
            @Value("${canvas.marketing-integrations.probe-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.marketing-integrations.probe-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.marketing-integrations.probe-scheduler.limit:100}") int limit,
            @Value("${canvas.marketing-integrations.probe-scheduler.operator:marketing-integration-probe-scheduler}") String operator) {
        this(automationService, null, enabled, tenantId, limit, operator, 120);
    }

    /**
     * 创建 MarketingIntegrationContractProbeScheduler 实例并注入 domain.marketing 场景依赖。
     * @param automationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 MarketingIntegrationContractProbeScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 MarketingIntegrationContractProbeScheduler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public MarketingIntegrationContractProbeScheduler(
            MarketingIntegrationContractProbeAutomationService automationService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.marketing-integrations.probe-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.marketing-integrations.probe-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.marketing-integrations.probe-scheduler.limit:100}") int limit,
            @Value("${canvas.marketing-integrations.probe-scheduler.operator:marketing-integration-probe-scheduler}") String operator,
            @Value("${canvas.marketing-integrations.probe-scheduler.lease-ttl-seconds:120}") long leaseTtlSeconds) {
        this.automationService = automationService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.limit = limit;
        this.operator = operator;
        this.leaseTtlSeconds = leaseTtlSeconds;
    }

    /**
     * 营销集成生产契约探测的 Spring 调度入口。
     *
     * <p>该入口只触发调度周期；实际业务由 {@link #runCycle()} 在租约和本地并发保护下委托自动化服务，
     * 扫描生产契约并执行探测。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.marketing-integrations.probe-scheduler.fixed-delay-ms:300000}")
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
            automationService.scanProductionContracts(tenantId, limit, operator);
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
