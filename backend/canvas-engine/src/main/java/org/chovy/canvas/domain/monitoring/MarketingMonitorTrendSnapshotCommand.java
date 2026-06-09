package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MarketingMonitorTrendSnapshotCommand 承载 domain.monitoring 场景中的不可变数据快照。
 * @param sourceId sourceId 字段。
 * @param bucketGrain bucketGrain 字段。
 * @param bucketStart bucketStart 字段。
 * @param bucketEnd bucketEnd 字段。
 * @param brandKey brandKey 字段。
 * @param competitorKey competitorKey 字段。
 * @param metadata metadata 字段。
 */
public record MarketingMonitorTrendSnapshotCommand(
        Long sourceId,
        String bucketGrain,
        LocalDateTime bucketStart,
        LocalDateTime bucketEnd,
        String brandKey,
        String competitorKey,
        Map<String, Object> metadata) {

    public MarketingMonitorTrendSnapshotCommand {
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
