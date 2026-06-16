package org.chovy.canvas.bi.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * BiDashboard 仪表盘模型。
 */
public record BiDashboard(
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
        BiResourceKey dashboardKey,
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
        BiResourceStatus status,
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
    public BiDashboard {
        tenantId = tenantId == null ? 0L : tenantId;
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        if (dashboardKey == null) {
            throw new IllegalArgumentException("dashboardKey is required");
        }
        name = name == null || name.isBlank() ? dashboardKey.value() : name.trim();
        theme = theme == null ? Map.of() : Map.copyOf(theme);
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        chartKeys = chartKeys == null ? List.of() : List.copyOf(chartKeys);
        status = status == null ? BiResourceStatus.DRAFT : status;
        version = Math.max(version, 1);
    }
    /**
     * 返回带有指定变更的新对象。
     */
    public BiDashboard withId(Long newId) {
        return new BiDashboard(newId, tenantId, workspaceId, dashboardKey, name, description, theme, filters,
                chartKeys, status, version, createdBy, createdAt, updatedAt);
    }
}
