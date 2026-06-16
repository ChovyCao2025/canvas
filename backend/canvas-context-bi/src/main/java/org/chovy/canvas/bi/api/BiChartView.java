package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;
/**
 * BiChartView 视图。
 */
public record BiChartView(
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
        String chartKey,
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
        String datasetKey,
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
    public BiChartView {
        query = query == null ? Map.of() : Map.copyOf(query);
        style = style == null ? Map.of() : Map.copyOf(style);
        interaction = interaction == null ? Map.of() : Map.copyOf(interaction);
    }
}
