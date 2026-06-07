package org.chovy.canvas.domain.bi.dataset;

import java.util.List;
import java.util.Map;

public record BiDatasetFromDatasourceCommand(
        Long dataSourceConfigId,
        String tableName,
        String datasetKey,
        String name,
        String tenantColumn,
        List<String> selectedColumns,
        Map<String, String> apiResponseVariables
) {
    public BiDatasetFromDatasourceCommand {
        selectedColumns = selectedColumns == null ? List.of() : List.copyOf(selectedColumns);
        apiResponseVariables = apiResponseVariables == null ? Map.of() : Map.copyOf(apiResponseVariables);
    }

    public BiDatasetFromDatasourceCommand(Long dataSourceConfigId,
                                          String tableName,
                                          String datasetKey,
                                          String name,
                                          String tenantColumn,
                                          List<String> selectedColumns) {
        this(dataSourceConfigId, tableName, datasetKey, name, tenantColumn, selectedColumns, Map.of());
    }
}
