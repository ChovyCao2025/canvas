package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

public record BiQuickEngineQueueJobView(
        Long id,
        Long tenantId,
        String poolKey,
        String sqlHash,
        String datasetKey,
        String requestedBy,
        String status,
        int attemptCount,
        LocalDateTime queuedAt,
        LocalDateTime expiresAt,
        String claimedBy,
        LocalDateTime claimedAt,
        LocalDateTime completedAt,
        String blockedReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
