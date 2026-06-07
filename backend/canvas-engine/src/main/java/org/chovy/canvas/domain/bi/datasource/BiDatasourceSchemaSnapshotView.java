package org.chovy.canvas.domain.bi.datasource;

import java.time.LocalDateTime;
import java.util.List;

public record BiDatasourceSchemaSnapshotView(
        Long id,
        Long dataSourceConfigId,
        String sourceKey,
        String name,
        String connectorType,
        String syncStatus,
        String errorMessage,
        Integer tableCount,
        Integer columnCount,
        List<BiDatasourceTablePreview> tables,
        LocalDateTime syncedAt,
        String syncedBy) {

    public BiDatasourceSchemaSnapshotView {
        tables = tables == null ? List.of() : List.copyOf(tables);
    }
}
