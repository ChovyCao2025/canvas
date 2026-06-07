package org.chovy.canvas.domain.bi.datasource;

public record BiDatasourceFileUploadCommand(
        String name,
        String description,
        String sheetName,
        String delimiter,
        Boolean headerRow,
        String encoding) {
}
