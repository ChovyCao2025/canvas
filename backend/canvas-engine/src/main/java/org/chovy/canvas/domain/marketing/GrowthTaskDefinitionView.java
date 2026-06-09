package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * GrowthTaskDefinitionView 承载 domain.marketing 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param activityId activityId 字段。
 * @param taskKey taskKey 字段。
 * @param taskType taskType 字段。
 * @param completionPolicy completionPolicy 字段。
 * @param resetPolicy resetPolicy 字段。
 * @param rewardPoolId rewardPoolId 字段。
 * @param targetValue targetValue 字段。
 * @param status status 字段。
 * @param rule rule 字段。
 * @param createdBy createdBy 字段。
 * @param updatedBy updatedBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record GrowthTaskDefinitionView(
        Long id,
        Long tenantId,
        Long activityId,
        String taskKey,
        String taskType,
        String completionPolicy,
        String resetPolicy,
        Long rewardPoolId,
        BigDecimal targetValue,
        String status,
        Map<String, Object> rule,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
