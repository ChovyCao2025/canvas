package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorTrendSnapshotQuery 承载 domain.monitoring 场景中的不可变数据快照。
 * @param sourceId sourceId 字段。
 * @param brandKey brandKey 字段。
 * @param competitorKey competitorKey 字段。
 * @param limit limit 字段。
 */
public record MarketingMonitorTrendSnapshotQuery(
        Long sourceId,
        String brandKey,
        String competitorKey,
        int limit) {
}
