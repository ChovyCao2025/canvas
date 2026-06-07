package org.chovy.canvas.domain.marketing;

import java.util.Map;

public record GrowthReferralRelationCommand(
        String referralCode,
        String inviteeUserId,
        Map<String, Object> riskEvidence) {
}
