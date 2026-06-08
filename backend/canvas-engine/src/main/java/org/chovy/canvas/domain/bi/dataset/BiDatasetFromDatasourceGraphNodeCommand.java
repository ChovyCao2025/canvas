package org.chovy.canvas.domain.bi.dataset;

/**
 * BiDatasetFromDatasourceGraphNodeCommand 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param tableName tableName 字段。
 * @param alias alias 字段。
 * @param x x 字段。
 * @param y y 字段。
 */
public record BiDatasetFromDatasourceGraphNodeCommand(
        String tableName,
        String alias,
        Integer x,
        Integer y
) {
}
