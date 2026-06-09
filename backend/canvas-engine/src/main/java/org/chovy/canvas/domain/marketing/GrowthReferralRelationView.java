package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * GrowthReferralRelationView 承载 domain.marketing 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param activityId activityId 字段。
 * @param referralCodeId referralCodeId 字段。
 * @param referrerParticipantId referrerParticipantId 字段。
 * @param inviteeUserId inviteeUserId 字段。
 * @param status status 字段。
 * @param riskEvidence riskEvidence 字段。
 * @param inviterRewardGrantId inviterRewardGrantId 字段。
 * @param inviteeRewardGrantId inviteeRewardGrantId 字段。
 * @param createdBy createdBy 字段。
 * @param updatedBy updatedBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record GrowthReferralRelationView(
        Long id,
        Long tenantId,
        Long activityId,
        Long referralCodeId,
        Long referrerParticipantId,
        String inviteeUserId,
        String status,
        Map<String, Object> riskEvidence,
        Long inviterRewardGrantId,
        Long inviteeRewardGrantId,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
