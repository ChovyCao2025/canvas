package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiQueryResultView 视图。
 */
public record BiQueryResultView(
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * columns 对应的数据集合。
         */
        List<Map<String, Object>> columns,
        /**
         * rows 对应的数据集合。
         */
        List<Map<String, Object>> rows,
        /**
         * rowCount 对应的统计数量。
         */
        int rowCount,
        /**
         * durationMs 对应的数据集合。
         */
        long durationMs,
        /**
         * sqlHash 字段值。
         */
        String sqlHash,
        boolean cached) {

    public BiQueryResultView {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
