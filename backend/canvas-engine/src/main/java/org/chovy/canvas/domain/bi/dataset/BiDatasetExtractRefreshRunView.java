package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

/**
 * BiDatasetExtractRefreshRunView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param id id 字段。
 * @param datasetKey datasetKey 字段。
 * @param status status 字段。
 * @param rowCount rowCount 字段。
 * @param durationMs durationMs 字段。
 * @param materializedTable materializedTable 字段。
 * @param requestedBy requestedBy 字段。
 * @param startedAt startedAt 字段。
 * @param finishedAt finishedAt 字段。
 * @param errorMessage errorMessage 字段。
 */
public record BiDatasetExtractRefreshRunView(
        Long id,
        String datasetKey,
        String status,
        Long rowCount,
        Long durationMs,
        String materializedTable,
        String requestedBy,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String errorMessage) {
}
