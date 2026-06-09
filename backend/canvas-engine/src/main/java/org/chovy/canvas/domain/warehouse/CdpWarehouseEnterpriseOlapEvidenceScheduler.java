package org.chovy.canvas.domain.warehouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CdpWarehouseEnterpriseOlapEvidenceScheduler 编排 domain.warehouse 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class CdpWarehouseEnterpriseOlapEvidenceScheduler {

    static final String LEASE_KEY = "CDP_WAREHOUSE_ENTERPRISE_OLAP_EVIDENCE";

    private final CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final String triggerType;
    private final String actor;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 CdpWarehouseEnterpriseOlapEvidenceScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param collectionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehouseEnterpriseOlapEvidenceScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param triggerType 类型标识，用于选择对应处理分支。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehouseEnterpriseOlapEvidenceScheduler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public CdpWarehouseEnterpriseOlapEvidenceScheduler(
            CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.enterprise-olap-evidence-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.enterprise-olap-evidence-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.enterprise-olap-evidence-scheduler.trigger-type:SCHEDULED}") String triggerType,
            @Value("${canvas.warehouse.enterprise-olap-evidence-scheduler.actor:enterprise-olap-evidence-scheduler}") String actor,
            @Value("${canvas.warehouse.enterprise-olap-evidence-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.collectionService = collectionService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId == null ? 0L : tenantId;
        this.triggerType = triggerType == null || triggerType.isBlank() ? "SCHEDULED" : triggerType.trim();
        this.actor = actor == null || actor.isBlank() ? "enterprise-olap-evidence-scheduler" : actor.trim();
        this.leaseTtlSeconds = Math.max(leaseTtlSeconds, 1);
    }

    /**
     * 企业级 OLAP 证据采集的 Spring 调度入口。
     *
     * <p>该入口只触发调度周期；实际业务由 {@link #runCycle()} 在租约和本地并发保护下委托采集服务，
     * 记录 Doris 指标、工作负载组和查询 SLO 等证据。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.warehouse.enterprise-olap-evidence-scheduler.fixed-delay-ms:300000}")
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
            collectionService.run(tenantId, triggerType, actor);
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
        return Duration.ofSeconds(leaseTtlSeconds);
    }
}
