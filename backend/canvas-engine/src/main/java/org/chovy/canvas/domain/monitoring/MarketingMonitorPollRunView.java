package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record MarketingMonitorPollRunView(
        Long id,
        Long tenantId,
        Long sourceId,
        String sourceKey,
        String sourceType,
        String status,
        LocalDateTime requestedFrom,
        LocalDateTime requestedUntil,
        String cursorBefore,
        String cursorAfter,
        int itemCount,
        int insertedCount,
        int duplicateCount,
        int alertCount,
        String errorMessage,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public MarketingMonitorPollRunView {
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
