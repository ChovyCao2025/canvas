package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record MarketingMonitorPollRequest(
        Long tenantId,
        Long sourceId,
        String sourceKey,
        String sourceType,
        String cursor,
        LocalDateTime requestedFrom,
        LocalDateTime requestedUntil,
        int maxItems,
        Map<String, Object> sourceMetadata) {

    public MarketingMonitorPollRequest {
        sourceMetadata = sourceMetadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(sourceMetadata));
    }
}
