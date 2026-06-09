package org.chovy.canvas.domain.warehouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CdpWarehousePrivacyAudienceBitmapRebuildScheduler 编排 domain.warehouse 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class CdpWarehousePrivacyAudienceBitmapRebuildScheduler {

    static final String LEASE_KEY = "CDP_WAREHOUSE_PRIVACY_AUDIENCE_REBUILD";

    private final CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService;
    private final CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService runService;
    private final CdpWarehouseJobLeaseService leaseService;
    private final boolean enabled;
    private final Long tenantId;
    private final int scanLimit;
    private final int audienceLimit;
    private final boolean retryFailed;
    private final String actor;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param automationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param scanLimit scan limit 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     * @param audienceLimit audience limit 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     * @param retryFailed retry failed 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehousePrivacyAudienceBitmapRebuildScheduler(
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService,
            CdpWarehouseJobLeaseService leaseService,
            boolean enabled,
            Long tenantId,
            int scanLimit,
            int audienceLimit,
            boolean retryFailed,
            String actor,
            long leaseTtlSeconds) {
        this(automationService, null, leaseService, enabled, tenantId, scanLimit, audienceLimit, retryFailed, actor,
                leaseTtlSeconds);
    }

    /**
     * 创建 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param automationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param scanLimit scan limit 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     * @param audienceLimit audience limit 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     * @param retryFailed retry failed 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     */
    public CdpWarehousePrivacyAudienceBitmapRebuildScheduler(
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService,
            CdpWarehouseJobLeaseService leaseService,
            boolean enabled,
            long tenantId,
            int scanLimit,
            int audienceLimit,
            boolean retryFailed,
            String actor,
            long leaseTtlSeconds) {
        this(automationService, leaseService, enabled, Long.valueOf(tenantId), scanLimit, audienceLimit, retryFailed,
                actor, leaseTtlSeconds);
    }

    /**
     * 创建 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 实例并注入 domain.warehouse 场景依赖。
     * @param automationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param scanLimit scan limit 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     * @param audienceLimit audience limit 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     * @param retryFailed retry failed 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 CdpWarehousePrivacyAudienceBitmapRebuildScheduler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public CdpWarehousePrivacyAudienceBitmapRebuildScheduler(
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService runService,
            CdpWarehouseJobLeaseService leaseService,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.enabled:false}") boolean enabled,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.tenant-id:0}") Long tenantId,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.scan-limit:50}") int scanLimit,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.audience-limit:100}") int audienceLimit,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.retry-failed:false}") boolean retryFailed,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.actor:privacy-rebuild-scheduler}") String actor,
            @Value("${canvas.warehouse.privacy-audience-rebuild-scheduler.lease-ttl-seconds:60}") long leaseTtlSeconds) {
        this.automationService = automationService;
        this.runService = runService;
        this.leaseService = leaseService;
        this.enabled = enabled;
        this.tenantId = tenantId == null ? 0L : tenantId;
        this.scanLimit = scanLimit;
        this.audienceLimit = audienceLimit;
        this.retryFailed = retryFailed;
        this.actor = actor == null || actor.isBlank() ? "privacy-rebuild-scheduler" : actor.trim();
        this.leaseTtlSeconds = Math.max(leaseTtlSeconds, 1);
    }

    /**
     * 隐私删除后人群 Bitmap 重建自动化的 Spring 调度入口。
     *
     * <p>该入口只触发调度周期；实际业务由 {@link #runCycle()} 在租约保护下扫描受影响人群，
     * 并委托自动化服务或带运行记录的服务执行重建。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.warehouse.privacy-audience-rebuild-scheduler.fixed-delay-ms:60000}")
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
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        try {
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand command =
                    new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand(
                            actor, scanLimit, audienceLimit, retryFailed);
            if (runService != null) {
                runService.runAndRecord(tenantId, command, "SCHEDULED");
            } else {
                automationService.run(tenantId, command);
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
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
