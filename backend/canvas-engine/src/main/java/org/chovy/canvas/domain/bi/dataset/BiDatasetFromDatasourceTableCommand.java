package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

public record BiDatasetFromDatasourceTableCommand(
        String tableName,
        String alias,
        List<String> selectedColumns
) {
    public BiDatasetFromDatasourceTableCommand {
        selectedColumns = selectedColumns == null ? List.of() : List.copyOf(selectedColumns);
    }
}
