package org.chovy.canvas.domain.marketing;

import java.util.Map;

/**
 * GrowthReferralRelationCommand 承载 domain.marketing 场景中的不可变数据快照。
 * @param referralCode referralCode 字段。
 * @param inviteeUserId inviteeUserId 字段。
 * @param riskEvidence riskEvidence 字段。
 */
public record GrowthReferralRelationCommand(
        String referralCode,
        String inviteeUserId,
        Map<String, Object> riskEvidence) {
}
