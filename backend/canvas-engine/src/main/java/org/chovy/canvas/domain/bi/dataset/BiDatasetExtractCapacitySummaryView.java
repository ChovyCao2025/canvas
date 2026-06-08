package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

/**
 * BiDatasetExtractCapacitySummaryView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param datasetKey datasetKey 字段。
 * @param enabled enabled 字段。
 * @param accelerationMode accelerationMode 字段。
 * @param refreshMode refreshMode 字段。
 * @param materializedTable materializedTable 字段。
 * @param lastStatus lastStatus 字段。
 * @param lastRefreshedAt lastRefreshedAt 字段。
 * @param retentionLimit retentionLimit 字段。
 * @param successfulRuns successfulRuns 字段。
 * @param failedRuns failedRuns 字段。
 * @param activeTables activeTables 字段。
 * @param droppedTables droppedTables 字段。
 * @param staleTables staleTables 字段。
 * @param retainedRows retainedRows 字段。
 * @param latestRowCount latestRowCount 字段。
 * @param latestDurationMs latestDurationMs 字段。
 */
public record BiDatasetExtractCapacitySummaryView(
        String datasetKey,
        Boolean enabled,
        String accelerationMode,
        String refreshMode,
        String materializedTable,
        String lastStatus,
        LocalDateTime lastRefreshedAt,
        Integer retentionLimit,
        Integer successfulRuns,
        Integer failedRuns,
        Integer activeTables,
        Integer droppedTables,
        Integer staleTables,
        Long retainedRows,
        Long latestRowCount,
        Long latestDurationMs) {
}
