package org.chovy.canvas.domain.programmatic;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ProgrammaticDspMutationView 承载 domain.programmatic 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param seatId seatId 字段。
 * @param campaignId campaignId 字段。
 * @param lineItemId lineItemId 字段。
 * @param supplyPathId supplyPathId 字段。
 * @param provider provider 字段。
 * @param mutationKey mutationKey 字段。
 * @param mutationType mutationType 字段。
 * @param entityType entityType 字段。
 * @param externalEntityId externalEntityId 字段。
 * @param requestHash requestHash 字段。
 * @param idempotencyKey idempotencyKey 字段。
 * @param status status 字段。
 * @param approvalStatus approvalStatus 字段。
 * @param dryRunRequired dryRunRequired 字段。
 * @param payload payload 字段。
 * @param validation validation 字段。
 * @param providerRequest providerRequest 字段。
 * @param providerResponse providerResponse 字段。
 * @param errorCode errorCode 字段。
 * @param errorMessage errorMessage 字段。
 * @param createdBy createdBy 字段。
 * @param approvedBy approvedBy 字段。
 * @param approvedAt approvedAt 字段。
 * @param executedBy executedBy 字段。
 * @param executedAt executedAt 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record ProgrammaticDspMutationView(
        Long id,
        Long tenantId,
        Long seatId,
        Long campaignId,
        Long lineItemId,
        Long supplyPathId,
        String provider,
        String mutationKey,
        String mutationType,
        String entityType,
        String externalEntityId,
        String requestHash,
        String idempotencyKey,
        String status,
        String approvalStatus,
        Boolean dryRunRequired,
        Map<String, Object> payload,
        Map<String, Object> validation,
        Map<String, Object> providerRequest,
        Map<String, Object> providerResponse,
        String errorCode,
        String errorMessage,
        String createdBy,
        String approvedBy,
        LocalDateTime approvedAt,
        String executedBy,
        LocalDateTime executedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
