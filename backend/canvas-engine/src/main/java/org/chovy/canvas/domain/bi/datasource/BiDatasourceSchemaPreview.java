package org.chovy.canvas.domain.bi.datasource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BiDatasourceSchemaPreview 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param id id 字段。
 * @param sourceKey sourceKey 字段。
 * @param name name 字段。
 * @param connectorType connectorType 字段。
 * @param tables tables 字段。
 * @param checkedAt checkedAt 字段。
 */
public record BiDatasourceSchemaPreview(
        Long id,
        String sourceKey,
        String name,
        String connectorType,
        List<BiDatasourceTablePreview> tables,
        LocalDateTime checkedAt) {

    public BiDatasourceSchemaPreview {
        tables = tables == null ? List.of() : List.copyOf(tables);
    }
}
