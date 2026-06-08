package org.chovy.canvas.domain.bi.dataset;

/**
 * BiDatasetExtractCleanupResultView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param datasetKey datasetKey 字段。
 * @param checkedTables checkedTables 字段。
 * @param retainedTables retainedTables 字段。
 * @param droppedTables droppedTables 字段。
 * @param failedDrops failedDrops 字段。
 */
public record BiDatasetExtractCleanupResultView(
        String datasetKey,
        Integer checkedTables,
        Integer retainedTables,
        Integer droppedTables,
        Integer failedDrops) {
}
