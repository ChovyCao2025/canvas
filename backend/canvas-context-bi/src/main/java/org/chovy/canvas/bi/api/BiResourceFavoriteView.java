package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiResourceFavoriteView 视图。
 */
public record BiResourceFavoriteView(
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 操作者。
         */
        String actor,
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        /**
         * 展示标题。
         */
        String title,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
