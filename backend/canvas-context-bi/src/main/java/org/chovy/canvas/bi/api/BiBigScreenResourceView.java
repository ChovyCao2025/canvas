package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * BiBigScreenResourceView 视图。
 */
public record BiBigScreenResourceView(
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * screenKey 对应的业务键。
         */
        String screenKey,
        /**
         * 展示标题。
         */
        String title,
        /**
         * 说明文本。
         */
        String description,
        /**
         * dashboardKeys 对应的数据集合。
         */
        List<String> dashboardKeys,
        /**
         * 布局配置。
         */
        Map<String, Object> layout,
        /**
         * settings 对应的数据集合。
         */
        Map<String, Object> settings,
        /**
         * 状态值。
         */
        String status,
        /**
         * 版本号。
         */
        Integer version,
        /**
         * 创建人。
         */
        String createdBy,
        /**
         * updatedBy 字段值。
         */
        String updatedBy,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
