package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

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
