package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiResourceCommentView 视图。
 */
public record BiResourceCommentView(
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
         * widgetKey 对应的业务键。
         */
        String widgetKey,
        /**
         * commentText 字段值。
         */
        String commentText,
        /**
         * 创建人。
         */
        String createdBy,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        LocalDateTime deletedAt) {
}
