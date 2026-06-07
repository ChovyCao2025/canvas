package org.chovy.canvas.domain.search;

import java.util.Map;

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
