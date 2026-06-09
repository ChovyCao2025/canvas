package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;

/**
 * GrowthReferralCodeView 承载 domain.marketing 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param activityId activityId 字段。
 * @param participantId participantId 字段。
 * @param code code 字段。
 * @param status status 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 */
public record GrowthReferralCodeView(
        Long id,
        Long tenantId,
        Long activityId,
        Long participantId,
        String code,
        String status,
        String createdBy,
        LocalDateTime createdAt) {
}
