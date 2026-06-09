package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MarketingMonitorTrendSnapshotView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param sourceKey sourceKey 字段。
 * @param bucketGrain bucketGrain 字段。
 * @param bucketStart bucketStart 字段。
 * @param bucketEnd bucketEnd 字段。
 * @param brandKey brandKey 字段。
 * @param competitorKey competitorKey 字段。
 * @param mentionCount mentionCount 字段。
 * @param positiveCount positiveCount 字段。
 * @param neutralCount neutralCount 字段。
 * @param negativeCount negativeCount 字段。
 * @param competitorCount competitorCount 字段。
 * @param alertCount alertCount 字段。
 * @param avgSentimentScore avgSentimentScore 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record MarketingMonitorTrendSnapshotView(
        Long id,
        Long tenantId,
        Long sourceId,
        String sourceKey,
        String bucketGrain,
        LocalDateTime bucketStart,
        LocalDateTime bucketEnd,
        String brandKey,
        String competitorKey,
        int mentionCount,
        int positiveCount,
        int neutralCount,
        int negativeCount,
        int competitorCount,
        int alertCount,
        BigDecimal avgSentimentScore,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public MarketingMonitorTrendSnapshotView {
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
