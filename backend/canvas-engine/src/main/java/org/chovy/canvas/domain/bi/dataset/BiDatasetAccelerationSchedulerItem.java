package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

/**
 * BiDatasetAccelerationSchedulerItem 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param datasetKey datasetKey 字段。
 * @param status status 字段。
 * @param reason reason 字段。
 * @param refreshRunId refreshRunId 字段。
 * @param rowCount rowCount 字段。
 * @param durationMs durationMs 字段。
 * @param materializedTable materializedTable 字段。
 * @param startedAt startedAt 字段。
 * @param finishedAt finishedAt 字段。
 */
public record BiDatasetAccelerationSchedulerItem(
        String datasetKey,
        String status,
        String reason,
        Long refreshRunId,
        Long rowCount,
        Long durationMs,
        String materializedTable,
        LocalDateTime startedAt,
        LocalDateTime finishedAt) {
}
