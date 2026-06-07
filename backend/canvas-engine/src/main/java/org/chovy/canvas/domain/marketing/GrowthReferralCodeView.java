package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;

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
