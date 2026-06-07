package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.util.Map;

public record GrowthRewardGrantCommand(
        Long poolId,
        Long participantId,
        Long referralRelationId,
        Long taskProgressId,
        String grantReason,
        String idempotencyKey,
        Map<String, Object> providerRequest,
        BigDecimal costAmount) {
}
