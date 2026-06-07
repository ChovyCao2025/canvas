package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

public record GrowthActivityEventView(
        Long id,
        Long tenantId,
        Long activityId,
        Long participantId,
        String eventType,
        String eventKey,
        String sourceType,
        Long sourceId,
        Map<String, Object> payload,
        String createdBy,
        LocalDateTime occurredAt) {
}
