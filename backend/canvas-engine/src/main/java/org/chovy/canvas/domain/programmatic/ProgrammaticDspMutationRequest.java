package org.chovy.canvas.domain.programmatic;

import java.util.Map;

public record ProgrammaticDspMutationRequest(
        Long tenantId,
        Long seatId,
        Long campaignId,
        Long lineItemId,
        Long supplyPathId,
        String provider,
        String seatKey,
        String advertiserAccountId,
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
