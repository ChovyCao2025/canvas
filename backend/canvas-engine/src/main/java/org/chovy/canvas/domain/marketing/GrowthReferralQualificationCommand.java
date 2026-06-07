package org.chovy.canvas.domain.marketing;

import java.util.Map;

public record GrowthReferralQualificationCommand(
        Long inviterRewardPoolId,
        Long inviteeRewardPoolId,
        Map<String, Object> riskEvidence) {
}
