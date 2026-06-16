package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiPublishApprovalView 视图。
 */
public record BiPublishApprovalView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 工作空间标识。
         */
        Long workspaceId,
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        /**
         * 状态值。
         */
        String status,
        /**
         * reason 字段值。
         */
        String reason,
        /**
         * requestedBy 字段值。
         */
        String requestedBy,
        /**
         * requestedAt 对应的时间。
         */
        LocalDateTime requestedAt,
        /**
         * reviewedBy 字段值。
         */
        String reviewedBy,
        /**
         * reviewedAt 对应的时间。
         */
        LocalDateTime reviewedAt,
        String reviewComment) {
}
