package org.chovy.canvas.domain.creator;

import java.util.Map;

/**
 * CreatorProviderMutationCommand 承载 domain.creator 场景中的不可变数据快照。
 * @param campaignId campaignId 字段。
 * @param collaborationId collaborationId 字段。
 * @param deliverableId deliverableId 字段。
 * @param mutationKey mutationKey 字段。
 * @param mutationType mutationType 字段。
 * @param entityType entityType 字段。
 * @param externalEntityId externalEntityId 字段。
 * @param dryRunRequired dryRunRequired 字段。
 * @param idempotencyKey idempotencyKey 字段。
 * @param payload payload 字段。
 */
public record CreatorProviderMutationCommand(
        Long campaignId,
        Long collaborationId,
        Long deliverableId,
        String mutationKey,
        String mutationType,
        String entityType,
        String externalEntityId,
        Boolean dryRunRequired,
        String idempotencyKey,
        Map<String, Object> payload
) {
}
