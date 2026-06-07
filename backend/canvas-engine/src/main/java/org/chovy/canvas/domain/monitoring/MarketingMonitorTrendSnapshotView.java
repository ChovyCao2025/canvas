package org.chovy.canvas.domain.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
