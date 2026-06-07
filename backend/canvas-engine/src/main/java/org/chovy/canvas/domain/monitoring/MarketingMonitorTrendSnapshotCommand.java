package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
