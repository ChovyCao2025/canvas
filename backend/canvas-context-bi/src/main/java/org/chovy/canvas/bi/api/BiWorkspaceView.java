package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiWorkspaceView 视图。
 */
public record BiWorkspaceView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 工作空间键。
         */
        String workspaceKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * 说明文本。
         */
        String description,
        /**
         * 状态值。
         */
        String status,
        /**
         * 创建人。
         */
        String createdBy,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        /**
         * 更新时间。
         */
        LocalDateTime updatedAt
) {
}
