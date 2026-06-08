package org.chovy.canvas.domain.bi.datasource;

/**
 * BiDatasourceFileUploadCommand 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param name name 字段。
 * @param description description 字段。
 * @param sheetName sheetName 字段。
 * @param delimiter delimiter 字段。
 * @param headerRow headerRow 字段。
 * @param encoding encoding 字段。
 */
public record BiDatasourceFileUploadCommand(
        String name,
        String description,
        String sheetName,
        String delimiter,
        Boolean headerRow,
        String encoding) {
}
