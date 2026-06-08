package org.chovy.canvas.domain.bi.dataset;

/**
 * BiDatasetExtractMaterializationResult 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param materializedTable materializedTable 字段。
 * @param rowCount rowCount 字段。
 * @param durationMs durationMs 字段。
 */
public record BiDatasetExtractMaterializationResult(
        String materializedTable,
        Long rowCount,
        Long durationMs) {
}
