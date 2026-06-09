package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * GrowthTaskProgressView 承载 domain.marketing 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param activityId activityId 字段。
 * @param participantId participantId 字段。
 * @param taskId taskId 字段。
 * @param progressValue progressValue 字段。
 * @param targetValue targetValue 字段。
 * @param status status 字段。
 * @param lastEventKey lastEventKey 字段。
 * @param evidence evidence 字段。
 * @param rewardGrantId rewardGrantId 字段。
 * @param updatedBy updatedBy 字段。
 * @param completedAt completedAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record GrowthTaskProgressView(
        Long id,
        Long tenantId,
        Long activityId,
        Long participantId,
        Long taskId,
        BigDecimal progressValue,
        BigDecimal targetValue,
        String status,
        String lastEventKey,
        Map<String, Object> evidence,
        Long rewardGrantId,
        String updatedBy,
        LocalDateTime completedAt,
        LocalDateTime updatedAt) {
}
