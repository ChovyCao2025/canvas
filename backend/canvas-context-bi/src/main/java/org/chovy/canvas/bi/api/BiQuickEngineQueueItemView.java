package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;

public record BiQuickEngineQueueItemView(
        Long id,
        Long tenantId,
        String poolKey,
        String sqlHash,
        String datasetKey,
        String requestedBy,
        String status,
        Integer attemptCount,
        LocalDateTime queuedAt,
        LocalDateTime expiresAt,
        String claimedBy,
        LocalDateTime claimedAt,
        LocalDateTime completedAt,
        String blockedReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
