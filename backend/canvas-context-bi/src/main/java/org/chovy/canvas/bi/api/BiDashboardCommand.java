package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiDashboardCommand 命令。
 */
public record BiDashboardCommand(
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
        String status
) {
    public BiDashboardCommand {
        theme = theme == null ? Map.of() : Map.copyOf(theme);
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        chartKeys = chartKeys == null ? List.of() : List.copyOf(chartKeys);
    }
}
