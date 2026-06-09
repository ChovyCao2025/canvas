package org.chovy.canvas.domain.marketing;

import java.util.Map;

/**
 * GrowthReferralQualificationCommand 承载 domain.marketing 场景中的不可变数据快照。
 * @param inviterRewardPoolId inviterRewardPoolId 字段。
 * @param inviteeRewardPoolId inviteeRewardPoolId 字段。
 * @param riskEvidence riskEvidence 字段。
 */
public record GrowthReferralQualificationCommand(
        Long inviterRewardPoolId,
        Long inviteeRewardPoolId,
        Map<String, Object> riskEvidence) {
}
