package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;
/**
 * BiResourceVersionView 视图。
 */
public record BiResourceVersionView(
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        /**
         * 版本号。
         */
        Integer version,
        /**
         * 状态值。
         */
        String status,
        /**
         * snapshot 字段值。
         */
        Map<String, Object> snapshot,
        /**
         * 创建人。
         */
        String createdBy,
        LocalDateTime createdAt) {
}
