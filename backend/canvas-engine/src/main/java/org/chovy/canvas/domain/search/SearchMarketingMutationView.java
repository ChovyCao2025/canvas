package org.chovy.canvas.domain.search;

import java.time.LocalDateTime;
import java.util.Map;

public record SearchMarketingMutationView(
        Long id,
        Long tenantId,
        Long sourceId,
        Long opportunityId,
        Long keywordId,
        String provider,
        String channel,
        String mutationKey,
        String mutationType,
        String entityType,
        String externalEntityId,
        String requestHash,
        String idempotencyKey,
        String status,
        String approvalStatus,
        boolean dryRunRequired,
        Map<String, Object> payload,
        Map<String, Object> validation,
        Map<String, Object> dryRunResult,
        Map<String, Object> providerResult,
        String errorCode,
        String errorMessage,
        String createdBy,
        String approvedBy,
        LocalDateTime approvedAt,
        String executedBy,
        LocalDateTime executedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
