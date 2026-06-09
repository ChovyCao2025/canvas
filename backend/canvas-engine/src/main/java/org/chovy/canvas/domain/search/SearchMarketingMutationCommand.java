package org.chovy.canvas.domain.search;

import java.util.Map;

/**
 * SearchMarketingMutationCommand 承载 domain.search 场景中的不可变数据快照。
 * @param sourceId sourceId 字段。
 * @param opportunityId opportunityId 字段。
 * @param keywordId keywordId 字段。
 * @param mutationKey mutationKey 字段。
 * @param mutationType mutationType 字段。
 * @param entityType entityType 字段。
 * @param externalEntityId externalEntityId 字段。
 * @param dryRunRequired dryRunRequired 字段。
 * @param idempotencyKey idempotencyKey 字段。
 * @param payload payload 字段。
 */
public record SearchMarketingMutationCommand(
        Long sourceId,
        Long opportunityId,
        Long keywordId,
        String mutationKey,
        String mutationType,
        String entityType,
        String externalEntityId,
        Boolean dryRunRequired,
        String idempotencyKey,
        Map<String, Object> payload) {
}
