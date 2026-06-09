package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MarketingMonitorPollRequest 承载 domain.monitoring 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param sourceKey sourceKey 字段。
 * @param sourceType sourceType 字段。
 * @param cursor cursor 字段。
 * @param requestedFrom requestedFrom 字段。
 * @param requestedUntil requestedUntil 字段。
 * @param maxItems maxItems 字段。
 * @param sourceMetadata sourceMetadata 字段。
 */
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
