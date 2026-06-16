package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * BiDashboardView 视图。
 */
public record BiDashboardView(
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
         * 仪表盘键。
         */
        String dashboardKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * 说明文本。
         */
        String description,
        /**
         * theme 字段值。
         */
        Map<String, Object> theme,
        /**
         * 筛选条件。
         */
        Map<String, Object> filters,
        /**
         * chartKeys 对应的数据集合。
         */
        List<String> chartKeys,
        /**
         * 状态值。
         */
        String status,
        /**
         * 版本号。
         */
        int version,
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
    public BiDashboardView {
        theme = theme == null ? Map.of() : Map.copyOf(theme);
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        chartKeys = chartKeys == null ? List.of() : List.copyOf(chartKeys);
    }
}
