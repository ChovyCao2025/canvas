package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

/**
 * BiDatasetFromDatasourceTableCommand 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param tableName tableName 字段。
 * @param alias alias 字段。
 * @param selectedColumns selectedColumns 字段。
 */
public record BiDatasetFromDatasourceTableCommand(
        String tableName,
        String alias,
        List<String> selectedColumns
) {
    public BiDatasetFromDatasourceTableCommand {
        selectedColumns = selectedColumns == null ? List.of() : List.copyOf(selectedColumns);
    }
}
