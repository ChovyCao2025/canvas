package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

/**
 * BiQuickEngineCapacityUsageDetailView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param type type 字段。
 * @param resourceKey resourceKey 字段。
 * @param usedRows usedRows 字段。
 * @param activeTables activeTables 字段。
 * @param latestRunId latestRunId 字段。
 * @param latestFinishedAt latestFinishedAt 字段。
 * @param latestRowCount latestRowCount 字段。
 * @param owner owner 字段。
 */
public record BiQuickEngineCapacityUsageDetailView(
        String type,
        String resourceKey,
        long usedRows,
        int activeTables,
        Long latestRunId,
        LocalDateTime latestFinishedAt,
        Long latestRowCount,
        String owner) {
}
