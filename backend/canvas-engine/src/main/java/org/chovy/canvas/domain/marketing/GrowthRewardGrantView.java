package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record GrowthRewardGrantView(
        Long id,
        Long tenantId,
        Long activityId,
        Long poolId,
        Long participantId,
        Long referralRelationId,
        Long taskProgressId,
        String grantReason,
        String status,
        String idempotencyKey,
        Map<String, Object> providerRequest,
        Map<String, Object> providerResponse,
        BigDecimal costAmount,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
