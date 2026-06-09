package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.util.Map;

/**
 * GrowthTaskDefinitionCommand 承载 domain.marketing 场景中的不可变数据快照。
 * @param taskKey taskKey 字段。
 * @param taskType taskType 字段。
 * @param completionPolicy completionPolicy 字段。
 * @param resetPolicy resetPolicy 字段。
 * @param rewardPoolId rewardPoolId 字段。
 * @param targetValue targetValue 字段。
 * @param status status 字段。
 * @param rule rule 字段。
 */
public record GrowthTaskDefinitionCommand(
        String taskKey,
        String taskType,
        String completionPolicy,
        String resetPolicy,
        Long rewardPoolId,
        BigDecimal targetValue,
        String status,
        Map<String, Object> rule) {
}
