package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiResourcePermissionView 视图。
 */
public record BiResourcePermissionView(
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
         * 资源标识。
         */
        Long resourceId,
        /**
         * subjectType 字段值。
         */
        String subjectType,
        /**
         * subjectId 对应的标识。
         */
        String subjectId,
        /**
         * actionKey 对应的业务键。
         */
        String actionKey,
        /**
         * effect 字段值。
         */
        String effect,
        /**
         * 创建人。
         */
        String createdBy,
        LocalDateTime createdAt) {
}
