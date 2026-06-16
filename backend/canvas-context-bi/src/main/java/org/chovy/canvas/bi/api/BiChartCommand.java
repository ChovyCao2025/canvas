package org.chovy.canvas.bi.api;

import java.util.Map;
/**
 * BiChartCommand 命令。
 */
public record BiChartCommand(
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
        String status
) {
    public BiChartCommand {
        query = query == null ? Map.of() : Map.copyOf(query);
        style = style == null ? Map.of() : Map.copyOf(style);
        interaction = interaction == null ? Map.of() : Map.copyOf(interaction);
    }
}
