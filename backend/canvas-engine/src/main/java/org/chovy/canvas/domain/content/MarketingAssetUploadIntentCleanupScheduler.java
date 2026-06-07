package org.chovy.canvas.domain.content;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
public class MarketingAssetUploadIntentCleanupScheduler {

    private final MarketingAssetUploadService uploadService;
    private final boolean enabled;
    private final Long tenantId;
    private final int limit;
    private final String actor;
    private final AtomicBoolean running = new AtomicBoolean(false);

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

    @Scheduled(fixedDelayString = "${canvas.marketing.content.asset-upload.cleanup.fixed-delay-ms:60000}")
    public void scheduledCycle() {
        runScheduledOnce();
    }

    public MarketingAssetUploadService.UploadIntentCleanupResult runScheduledOnce() {
        if (!enabled) {
            return empty();
        }
        if (!running.compareAndSet(false, true)) {
            return empty();
        }
        try {
            return uploadService.expireStalePendingUploads(
                    new TenantContext(tenantId, RoleNames.TENANT_ADMIN, actor),
                    new MarketingAssetUploadService.UploadIntentCleanupCommand(limit, actor));
        } finally {
            running.set(false);
        }
    }

    private MarketingAssetUploadService.UploadIntentCleanupResult empty() {
        return new MarketingAssetUploadService.UploadIntentCleanupResult(0, 0, null);
    }
}
