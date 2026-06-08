package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

/**
 * BiQuickEngineQueueJobView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param poolKey poolKey 字段。
 * @param sqlHash sqlHash 字段。
 * @param datasetKey datasetKey 字段。
 * @param requestedBy requestedBy 字段。
 * @param status status 字段。
 * @param attemptCount attemptCount 字段。
 * @param queuedAt queuedAt 字段。
 * @param expiresAt expiresAt 字段。
 * @param claimedBy claimedBy 字段。
 * @param claimedAt claimedAt 字段。
 * @param completedAt completedAt 字段。
 * @param blockedReason blockedReason 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
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
