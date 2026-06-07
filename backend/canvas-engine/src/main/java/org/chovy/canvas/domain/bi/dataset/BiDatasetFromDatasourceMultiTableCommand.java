package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

public record BiDatasetFromDatasourceMultiTableCommand(
        Long dataSourceConfigId,
        String datasetKey,
        String name,
        String baseTableName,
        String tenantColumn,
        List<BiDatasetFromDatasourceTableCommand> tables,
        List<BiDatasetFromDatasourceJoinCommand> joins,
        BiDatasetFromDatasourceGraphCommand graph
) {
    public BiDatasetFromDatasourceMultiTableCommand(Long dataSourceConfigId,
                                                    String datasetKey,
                                                    String name,
                                                    String baseTableName,
                                                    String tenantColumn,
                                                    List<BiDatasetFromDatasourceTableCommand> tables,
                                                    List<BiDatasetFromDatasourceJoinCommand> joins) {
        this(dataSourceConfigId, datasetKey, name, baseTableName, tenantColumn, tables, joins, null);
    }

    public BiDatasetFromDatasourceMultiTableCommand {
        tables = tables == null ? List.of() : List.copyOf(tables);
        joins = joins == null ? List.of() : List.copyOf(joins);
    }
}
