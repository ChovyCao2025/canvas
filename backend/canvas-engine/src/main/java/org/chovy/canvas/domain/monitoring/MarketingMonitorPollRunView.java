package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MarketingMonitorPollRunView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param sourceKey sourceKey 字段。
 * @param sourceType sourceType 字段。
 * @param status status 字段。
 * @param requestedFrom requestedFrom 字段。
 * @param requestedUntil requestedUntil 字段。
 * @param cursorBefore cursorBefore 字段。
 * @param cursorAfter cursorAfter 字段。
 * @param itemCount itemCount 字段。
 * @param insertedCount insertedCount 字段。
 * @param duplicateCount duplicateCount 字段。
 * @param alertCount alertCount 字段。
 * @param errorMessage errorMessage 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param startedAt startedAt 字段。
 * @param finishedAt finishedAt 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
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
