package org.chovy.canvas.domain.search;

import java.util.Map;

/**
 * SearchMarketingProviderMutationRequest 承载 domain.search 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param provider provider 字段。
 * @param sourceKey sourceKey 字段。
 * @param externalAccountId externalAccountId 字段。
 * @param mutationType mutationType 字段。
 * @param entityType entityType 字段。
 * @param externalEntityId externalEntityId 字段。
 * @param idempotencyKey idempotencyKey 字段。
 * @param dryRun dryRun 字段。
 * @param partialFailure partialFailure 字段。
 * @param payload payload 字段。
 * @param metadata metadata 字段。
 */
public record SearchMarketingProviderMutationRequest(
        Long tenantId,
        Long sourceId,
        String provider,
        String sourceKey,
        String externalAccountId,
        String mutationType,
        String entityType,
        String externalEntityId,
        String idempotencyKey,
        boolean dryRun,
        boolean partialFailure,
        Map<String, Object> payload,
        Map<String, Object> metadata
) {
}
