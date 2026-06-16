package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiResourceLocationView 视图。
 */
public record BiResourceLocationView(
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
         * folderKey 对应的业务键。
         */
        String folderKey,
        /**
         * sortOrder 字段值。
         */
        Integer sortOrder,
        /**
         * movedBy 字段值。
         */
        String movedBy,
        LocalDateTime movedAt) {
}
