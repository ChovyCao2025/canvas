package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * GrowthActivityEventView 承载 domain.marketing 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param activityId activityId 字段。
 * @param participantId participantId 字段。
 * @param eventType eventType 字段。
 * @param eventKey eventKey 字段。
 * @param sourceType sourceType 字段。
 * @param sourceId sourceId 字段。
 * @param payload payload 字段。
 * @param createdBy createdBy 字段。
 * @param occurredAt occurredAt 字段。
 */
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
