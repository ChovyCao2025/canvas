package org.chovy.canvas.domain.bi.datasource;

/**
 * BiDatasourceColumnPreview 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param name name 字段。
 * @param typeName typeName 字段。
 * @param dataType dataType 字段。
 * @param nullable nullable 字段。
 * @param ordinalPosition ordinalPosition 字段。
 */
public record BiDatasourceColumnPreview(
        String name,
        String typeName,
        int dataType,
        boolean nullable,
        int ordinalPosition) {
}
