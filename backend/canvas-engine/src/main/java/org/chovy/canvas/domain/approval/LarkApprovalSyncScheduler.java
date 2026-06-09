package org.chovy.canvas.domain.approval;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LarkApprovalSyncScheduler 编排 domain.approval 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class LarkApprovalSyncScheduler {

    private final ApprovalWorkflowService workflowService;
    private final boolean enabled;
    private final Long tenantId;
    private final int limit;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 LarkApprovalSyncScheduler 实例并注入 domain.approval 场景依赖。
     * @param workflowService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 LarkApprovalSyncScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     */
    public LarkApprovalSyncScheduler(
            ApprovalWorkflowService workflowService,
            @Value("${canvas.approval.lark.sync.enabled:false}") boolean enabled,
            @Value("${canvas.approval.lark.sync.tenant-id:0}") Long tenantId,
            @Value("${canvas.approval.lark.sync.limit:100}") int limit) {
        this.workflowService = workflowService;
        this.enabled = enabled;
        this.tenantId = tenantId == null ? 0L : tenantId;
        this.limit = Math.max(1, Math.min(limit <= 0 ? 100 : limit, 1000));
    }

    /**
     * 飞书审批状态同步的 Spring 调度入口。
     *
     * <p>调度器按配置的固定延迟触发，本方法本身不做业务扫描，只委托 {@link #runScheduledOnce()} 完成开关判断、
     * 并发保护和待同步外部审批实例处理。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.approval.lark.sync.fixed-delay-ms:60000}")
    public void scheduledCycle() {
        runScheduledOnce();
    }

    /**
     * 执行一次飞书审批状态同步。
     *
     * <p>方法会在配置关闭或已有一轮同步运行时直接跳过；实际副作用是调用工作流服务同步指定租户下挂起的外部审批实例，
     * 并更新本地审批实例和任务状态。</p>
     *
     * @return 本轮是否真正执行，以及成功委托同步的实例数量
     */
    public SyncRunResult runScheduledOnce() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!enabled) {
            return new SyncRunResult(false, 0);
        }
        if (!running.compareAndSet(false, true)) {
            return new SyncRunResult(false, 0);
        }
        try {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new SyncRunResult(true, workflowService.syncPendingExternalInstances(tenantId, limit));
        } finally {
            running.set(false);
        }
    }

    /**
     * SyncRunResult 创建或触发 domain.approval 场景的业务处理。
     * @param executed executed 参数，用于 SyncRunResult 流程中的校验、计算或对象转换。
     * @param synced synced 参数，用于 SyncRunResult 流程中的校验、计算或对象转换。
     * @return 返回 SyncRunResult 流程生成的业务结果。
     */
    public record SyncRunResult(boolean executed, int synced) {
    }
}
