package org.chovy.canvas.domain.search;

import java.util.Map;

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
