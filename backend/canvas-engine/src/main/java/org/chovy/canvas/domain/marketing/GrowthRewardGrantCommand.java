package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.util.Map;

/**
 * GrowthRewardGrantCommand 承载 domain.marketing 场景中的不可变数据快照。
 * @param poolId poolId 字段。
 * @param participantId participantId 字段。
 * @param referralRelationId referralRelationId 字段。
 * @param taskProgressId taskProgressId 字段。
 * @param grantReason grantReason 字段。
 * @param idempotencyKey idempotencyKey 字段。
 * @param providerRequest providerRequest 字段。
 * @param costAmount costAmount 字段。
 */
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
