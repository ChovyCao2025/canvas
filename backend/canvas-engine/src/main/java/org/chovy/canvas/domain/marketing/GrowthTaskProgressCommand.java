package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.util.Map;

/**
 * GrowthTaskProgressCommand 承载 domain.marketing 场景中的不可变数据快照。
 * @param taskId taskId 字段。
 * @param participantId participantId 字段。
 * @param deltaValue deltaValue 字段。
 * @param eventKey eventKey 字段。
 * @param evidence evidence 字段。
 */
public record GrowthTaskProgressCommand(
        Long taskId,
        Long participantId,
        BigDecimal deltaValue,
        String eventKey,
        Map<String, Object> evidence) {
}
