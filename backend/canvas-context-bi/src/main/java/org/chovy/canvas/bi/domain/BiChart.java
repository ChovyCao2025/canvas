package org.chovy.canvas.bi.domain;

import java.time.LocalDateTime;
import java.util.Map;
/**
 * BiChart 图表模型。
 */
public record BiChart(
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
         * 图表键。
         */
        BiResourceKey chartKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * chartType 字段值。
         */
        String chartType,
        /**
         * 数据集标识。
         */
        Long datasetId,
        /**
         * 数据集键。
         */
        BiResourceKey datasetKey,
        /**
         * 查询定义。
         */
        Map<String, Object> query,
        /**
         * 样式配置。
         */
        Map<String, Object> style,
        /**
         * 交互配置。
         */
        Map<String, Object> interaction,
        /**
         * 状态值。
         */
        BiResourceStatus status,
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
    public BiChart {
        tenantId = tenantId == null ? 0L : tenantId;
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        if (chartKey == null) {
            throw new IllegalArgumentException("chartKey is required");
        }
        if (datasetId == null || datasetId <= 0 || datasetKey == null) {
            throw new IllegalArgumentException("chart dataset is required");
        }
        name = name == null || name.isBlank() ? chartKey.value() : name.trim();
        chartType = chartType == null || chartType.isBlank()
                ? "TABLE"
                : chartType.trim().toUpperCase(java.util.Locale.ROOT);
        query = query == null ? Map.of() : Map.copyOf(query);
        style = style == null ? Map.of() : Map.copyOf(style);
        interaction = interaction == null ? Map.of() : Map.copyOf(interaction);
        status = status == null ? BiResourceStatus.DRAFT : status;
    }
    /**
     * 返回带有指定变更的新对象。
     */
    public BiChart withId(Long newId) {
        return new BiChart(newId, tenantId, workspaceId, chartKey, name, chartType, datasetId, datasetKey,
                query, style, interaction, status, createdBy, createdAt, updatedAt);
    }
}
