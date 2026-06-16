package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiPermissionRequestView 视图。
 */
public record BiPermissionRequestView(
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
         * requestedAction 字段值。
         */
        String requestedAction,
        /**
         * requestedBy 字段值。
         */
        String requestedBy,
        /**
         * requestedAt 对应的时间。
         */
        LocalDateTime requestedAt,
        /**
         * reason 字段值。
         */
        String reason,
        /**
         * 状态值。
         */
        String status,
        /**
         * reviewedBy 字段值。
         */
        String reviewedBy,
        /**
         * reviewedAt 对应的时间。
         */
        LocalDateTime reviewedAt,
        /**
         * reviewComment 字段值。
         */
        String reviewComment,
        Long grantedPermissionId) {
}
