package org.chovy.canvas.domain.content;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MarketingAssetUploadIntentCleanupScheduler 编排 domain.content 场景的领域业务规则。
 */
@Service
@EnableScheduling
public class MarketingAssetUploadIntentCleanupScheduler {

    private final MarketingAssetUploadService uploadService;
    private final boolean enabled;
    private final Long tenantId;
    private final int limit;
    private final String actor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建 MarketingAssetUploadIntentCleanupScheduler 实例并注入 domain.content 场景依赖。
     * @param uploadService 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 MarketingAssetUploadIntentCleanupScheduler 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    public MarketingAssetUploadIntentCleanupScheduler(
            MarketingAssetUploadService uploadService,
            @Value("${canvas.marketing.content.asset-upload.cleanup.enabled:false}") boolean enabled,
            @Value("${canvas.marketing.content.asset-upload.cleanup.tenant-id:0}") Long tenantId,
            @Value("${canvas.marketing.content.asset-upload.cleanup.limit:100}") int limit,
            @Value("${canvas.marketing.content.asset-upload.cleanup.actor:asset-upload-cleanup-scheduler}") String actor) {
        this.uploadService = uploadService;
        this.enabled = enabled;
        this.tenantId = tenantId == null ? 0L : tenantId;
        this.limit = limit <= 0 ? 100 : Math.min(limit, 500);
        this.actor = actor == null || actor.isBlank() ? "asset-upload-cleanup-scheduler" : actor.trim();
    }

    /**
     * 营销素材上传意图清理的 Spring 调度入口。
     *
     * <p>该入口只按固定延迟触发 {@link #runScheduledOnce()}；实际业务是以配置的租户和调度器身份过期长期停留在
     * pending 状态的上传意图。</p>
     */
    @Scheduled(fixedDelayString = "${canvas.marketing.content.asset-upload.cleanup.fixed-delay-ms:60000}")
    public void scheduledCycle() {
        runScheduledOnce();
    }

    /**
     * 执行一次上传意图清理。
     *
     * <p>方法会检查开关并通过 {@link AtomicBoolean} 防止重入；真正执行时调用上传服务过期 stale pending intent，
     * 可能更新上传意图状态和审计信息。关闭或已有任务运行时返回空清理结果。</p>
     *
     * @return 本轮扫描和过期的上传意图数量，以及上传服务返回的游标信息
     */
    public MarketingAssetUploadService.UploadIntentCleanupResult runScheduledOnce() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!enabled) {
            return empty();
        }
        if (!running.compareAndSet(false, true)) {
            return empty();
        }
        try {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return uploadService.expireStalePendingUploads(
                    new TenantContext(tenantId, RoleNames.TENANT_ADMIN, actor),
                    new MarketingAssetUploadService.UploadIntentCleanupCommand(limit, actor));
        } finally {
            running.set(false);
        }
    }

    /**
     * 执行 empty 流程，围绕 empty 完成校验、计算或结果组装。
     *
     * @return 返回 empty 流程生成的业务结果。
     */
    private MarketingAssetUploadService.UploadIntentCleanupResult empty() {
        return new MarketingAssetUploadService.UploadIntentCleanupResult(0, 0, null);
    }
}
