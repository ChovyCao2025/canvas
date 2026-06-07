package org.chovy.canvas.domain.approval;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
public class LarkApprovalSyncScheduler {

    private final ApprovalWorkflowService workflowService;
    private final boolean enabled;
    private final Long tenantId;
    private final int limit;
    private final AtomicBoolean running = new AtomicBoolean(false);

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

    @Scheduled(fixedDelayString = "${canvas.approval.lark.sync.fixed-delay-ms:60000}")
    public void scheduledCycle() {
        runScheduledOnce();
    }

    public SyncRunResult runScheduledOnce() {
        if (!enabled) {
            return new SyncRunResult(false, 0);
        }
        if (!running.compareAndSet(false, true)) {
            return new SyncRunResult(false, 0);
        }
        try {
            return new SyncRunResult(true, workflowService.syncPendingExternalInstances(tenantId, limit));
        } finally {
            running.set(false);
        }
    }

    public record SyncRunResult(boolean executed, int synced) {
    }
}
