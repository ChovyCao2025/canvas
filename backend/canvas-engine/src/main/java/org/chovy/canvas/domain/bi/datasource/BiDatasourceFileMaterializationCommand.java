package org.chovy.canvas.domain.bi.datasource;

public record BiDatasourceFileMaterializationCommand(
        String name,
        String description,
        String sheetName,
        String delimiter,
        Boolean headerRow,
        String encoding,
        String datasetKey,
        String datasetName,
        String tenantColumn,
        Integer schemaLimit,
        Long maxRows
) {
}
