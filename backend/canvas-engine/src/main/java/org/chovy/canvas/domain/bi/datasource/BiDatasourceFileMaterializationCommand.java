package org.chovy.canvas.domain.bi.datasource;

/**
 * BiDatasourceFileMaterializationCommand 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param name name 字段。
 * @param description description 字段。
 * @param sheetName sheetName 字段。
 * @param delimiter delimiter 字段。
 * @param headerRow headerRow 字段。
 * @param encoding encoding 字段。
 * @param datasetKey datasetKey 字段。
 * @param datasetName datasetName 字段。
 * @param tenantColumn tenantColumn 字段。
 * @param schemaLimit schemaLimit 字段。
 * @param maxRows maxRows 字段。
 */
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
