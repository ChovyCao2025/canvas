package org.chovy.canvas.domain.search;

import java.util.Map;

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
