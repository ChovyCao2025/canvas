package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record BiDatasourceSchemaSnapshotView(
        Long id,
        Long dataSourceConfigId,
        String sourceKey,
        String syncStatus,
        String syncedBy,
        int tableCount,
        List<Map<String, Object>> tables,
        LocalDateTime syncedAt) {

    public BiDatasourceSchemaSnapshotView {
        tables = tables == null ? List.of() : List.copyOf(tables);
    }
}
