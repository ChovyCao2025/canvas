package org.chovy.canvas.domain.creator;

import java.util.Map;

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
