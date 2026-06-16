package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiResourceLockView 视图。
 */
public record BiResourceLockView(
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
         * lockToken 字段值。
         */
        String lockToken,
        /**
         * lockedBy 字段值。
         */
        String lockedBy,
        /**
         * lockedAt 对应的时间。
         */
        LocalDateTime lockedAt,
        /**
         * expiresAt 对应的时间。
         */
        LocalDateTime expiresAt,
        Boolean locked) {
}
