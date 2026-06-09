package org.chovy.canvas.domain.marketing;

import java.util.Map;

/**
 * GrowthActivityEventCommand 承载 domain.marketing 场景中的不可变数据快照。
 * @param participantId participantId 字段。
 * @param eventType eventType 字段。
 * @param eventKey eventKey 字段。
 * @param sourceType sourceType 字段。
 * @param sourceId sourceId 字段。
 * @param payload payload 字段。
 */
public record GrowthActivityEventCommand(
        Long participantId,
        String eventType,
        String eventKey,
        String sourceType,
        Long sourceId,
        Map<String, Object> payload) {
}
