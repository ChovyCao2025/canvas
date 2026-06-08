package org.chovy.canvas.domain.bi.datasource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BiDatasourceSchemaSnapshotView 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param id id 字段。
 * @param dataSourceConfigId dataSourceConfigId 字段。
 * @param sourceKey sourceKey 字段。
 * @param name name 字段。
 * @param connectorType connectorType 字段。
 * @param syncStatus syncStatus 字段。
 * @param errorMessage errorMessage 字段。
 * @param tableCount tableCount 字段。
 * @param columnCount columnCount 字段。
 * @param tables tables 字段。
 * @param syncedAt syncedAt 字段。
 * @param syncedBy syncedBy 字段。
 */
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
