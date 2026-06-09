package org.chovy.canvas.domain.search;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * SearchMarketingMutationView 承载 domain.search 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param opportunityId opportunityId 字段。
 * @param keywordId keywordId 字段。
 * @param provider provider 字段。
 * @param channel channel 字段。
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
 * @param dryRunResult dryRunResult 字段。
 * @param providerResult providerResult 字段。
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
public record SearchMarketingMutationView(
        Long id,
        Long tenantId,
        Long sourceId,
        Long opportunityId,
        Long keywordId,
        String provider,
        String channel,
        String mutationKey,
        String mutationType,
        String entityType,
        String externalEntityId,
        String requestHash,
        String idempotencyKey,
        String status,
        String approvalStatus,
        boolean dryRunRequired,
        Map<String, Object> payload,
        Map<String, Object> validation,
        Map<String, Object> dryRunResult,
        Map<String, Object> providerResult,
        String errorCode,
        String errorMessage,
        String createdBy,
        String approvedBy,
        LocalDateTime approvedAt,
        String executedBy,
        LocalDateTime executedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
