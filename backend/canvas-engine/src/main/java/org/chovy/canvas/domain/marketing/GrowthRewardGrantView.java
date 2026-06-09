package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * GrowthRewardGrantView 承载 domain.marketing 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param activityId activityId 字段。
 * @param poolId poolId 字段。
 * @param participantId participantId 字段。
 * @param referralRelationId referralRelationId 字段。
 * @param taskProgressId taskProgressId 字段。
 * @param grantReason grantReason 字段。
 * @param status status 字段。
 * @param idempotencyKey idempotencyKey 字段。
 * @param providerRequest providerRequest 字段。
 * @param providerResponse providerResponse 字段。
 * @param costAmount costAmount 字段。
 * @param createdBy createdBy 字段。
 * @param updatedBy updatedBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
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
