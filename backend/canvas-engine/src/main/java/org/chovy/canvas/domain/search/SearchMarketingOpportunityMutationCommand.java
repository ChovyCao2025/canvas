package org.chovy.canvas.domain.search;

import java.util.Map;

/**
 * SearchMarketingOpportunityMutationCommand 承载 domain.search 场景中的不可变数据快照。
 * @param mutationKey mutationKey 字段。
 * @param mutationType mutationType 字段。
 * @param entityType entityType 字段。
 * @param externalEntityId externalEntityId 字段。
 * @param dryRunRequired dryRunRequired 字段。
 * @param idempotencyKey idempotencyKey 字段。
 * @param payload payload 字段。
 */
public record SearchMarketingOpportunityMutationCommand(
        String mutationKey,
        String mutationType,
        String entityType,
        String externalEntityId,
        Boolean dryRunRequired,
        String idempotencyKey,
        Map<String, Object> payload) {

    public SearchMarketingOpportunityMutationCommand {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
