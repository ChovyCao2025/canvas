package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiQueryCommand 命令。
 */
public record BiQueryCommand(
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * 仪表盘键。
         */
        String dashboardKey,
        /**
         * dimensions 对应的数据集合。
         */
        List<String> dimensions,
        /**
         * 指标列表。
         */
        List<String> metrics,
        /**
         * 筛选条件。
         */
        List<Map<String, Object>> filters,
        /**
         * sorts 对应的数据集合。
         */
        List<Map<String, Object>> sorts,
        /**
         * 返回数量上限。
         */
        int limit,
        /**
         * offset 字段值。
         */
        int offset,
        Map<String, String> sqlParameters) {

    public BiQueryCommand {
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        filters = filters == null ? List.of() : List.copyOf(filters);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
        sqlParameters = sqlParameters == null ? Map.of() : Map.copyOf(sqlParameters);
        offset = Math.max(0, offset);
    }
}
