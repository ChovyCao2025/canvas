package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiResourceOwnershipView 视图。
 */
public record BiResourceOwnershipView(
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
         * ownerUser 字段值。
         */
        String ownerUser,
        /**
         * transferredBy 字段值。
         */
        String transferredBy,
        LocalDateTime transferredAt) {
}
