package org.chovy.canvas.domain.creator;

import java.util.Map;

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
