package org.chovy.canvas.domain.programmatic;

import java.util.Map;

public record ProgrammaticDspMutationCommand(
        Long seatId,
        Long campaignId,
        Long lineItemId,
        Long supplyPathId,
        String mutationKey,
        String mutationType,
        String entityType,
        String externalEntityId,
        Boolean dryRunRequired,
        String idempotencyKey,
        Map<String, Object> payload
) {
}
