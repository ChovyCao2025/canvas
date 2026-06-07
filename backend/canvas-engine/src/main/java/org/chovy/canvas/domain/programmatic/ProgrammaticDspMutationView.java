package org.chovy.canvas.domain.programmatic;

import java.time.LocalDateTime;
import java.util.Map;

public record ProgrammaticDspMutationView(
        Long id,
        Long tenantId,
        Long seatId,
        Long campaignId,
        Long lineItemId,
        Long supplyPathId,
        String provider,
        String mutationKey,
        String mutationType,
        String entityType,
        String externalEntityId,
        String requestHash,
        String idempotencyKey,
        String status,
        String approvalStatus,
        Boolean dryRunRequired,
        Map<String, Object> payload,
        Map<String, Object> validation,
        Map<String, Object> providerRequest,
        Map<String, Object> providerResponse,
        String errorCode,
        String errorMessage,
        String createdBy,
        String approvedBy,
        LocalDateTime approvedAt,
        String executedBy,
        LocalDateTime executedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
