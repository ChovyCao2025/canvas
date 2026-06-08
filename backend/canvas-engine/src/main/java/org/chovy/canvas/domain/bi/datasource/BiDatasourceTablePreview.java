package org.chovy.canvas.domain.bi.datasource;

import java.util.List;

/**
 * BiDatasourceTablePreview 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param name name 字段。
 * @param tableType tableType 字段。
 * @param columns columns 字段。
 */
public record BiDatasourceTablePreview(
        String name,
        String tableType,
        List<BiDatasourceColumnPreview> columns) {

    public BiDatasourceTablePreview {
        columns = columns == null ? List.of() : List.copyOf(columns);
    }
}
