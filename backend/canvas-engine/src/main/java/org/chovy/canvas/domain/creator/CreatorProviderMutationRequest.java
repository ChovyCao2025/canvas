package org.chovy.canvas.domain.creator;

import java.util.Map;

/**
 * CreatorProviderMutationRequest 承载 domain.creator 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param campaignId campaignId 字段。
 * @param collaborationId collaborationId 字段。
 * @param deliverableId deliverableId 字段。
 * @param creatorId creatorId 字段。
 * @param provider provider 字段。
 * @param mutationType mutationType 字段。
 * @param entityType entityType 字段。
 * @param externalEntityId externalEntityId 字段。
 * @param idempotencyKey idempotencyKey 字段。
 * @param dryRun dryRun 字段。
 * @param partialFailure partialFailure 字段。
 * @param payload payload 字段。
 * @param metadata metadata 字段。
 */
public record CreatorProviderMutationRequest(
        Long tenantId,
        Long campaignId,
        Long collaborationId,
        Long deliverableId,
        Long creatorId,
        String provider,
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
