package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiQuickEngineQueueItemView 视图。
 */
public record BiQuickEngineQueueItemView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * poolKey 对应的业务键。
         */
        String poolKey,
        /**
         * sqlHash 字段值。
         */
        String sqlHash,
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * requestedBy 字段值。
         */
        String requestedBy,
        /**
         * 状态值。
         */
        String status,
        /**
         * attemptCount 对应的统计数量。
         */
        Integer attemptCount,
        /**
         * queuedAt 对应的时间。
         */
        LocalDateTime queuedAt,
        /**
         * expiresAt 对应的时间。
         */
        LocalDateTime expiresAt,
        /**
         * claimedBy 字段值。
         */
        String claimedBy,
        /**
         * claimedAt 对应的时间。
         */
        LocalDateTime claimedAt,
        /**
         * completedAt 对应的时间。
         */
        LocalDateTime completedAt,
        /**
         * blockedReason 字段值。
         */
        String blockedReason,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
