package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * BiDatasourceSchemaSnapshotView 视图。
 */
public record BiDatasourceSchemaSnapshotView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * dataSourceConfigId 对应的标识。
         */
        Long dataSourceConfigId,
        /**
         * sourceKey 对应的业务键。
         */
        String sourceKey,
        /**
         * syncStatus 对应的数据集合。
         */
        String syncStatus,
        /**
         * syncedBy 字段值。
         */
        String syncedBy,
        /**
         * tableCount 对应的统计数量。
         */
        int tableCount,
        /**
         * tables 对应的数据集合。
         */
        List<Map<String, Object>> tables,
        LocalDateTime syncedAt) {

    public BiDatasourceSchemaSnapshotView {
        tables = tables == null ? List.of() : List.copyOf(tables);
    }
}
