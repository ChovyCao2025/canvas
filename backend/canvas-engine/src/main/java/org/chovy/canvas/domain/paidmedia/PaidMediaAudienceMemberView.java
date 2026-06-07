package org.chovy.canvas.domain.paidmedia;

import java.time.LocalDateTime;

public record PaidMediaAudienceMemberView(
        Long id,
        Long tenantId,
        Long runId,
        Long destinationId,
        Long audienceId,
        String provider,
        String userId,
        String identifierType,
        String identifierHash,
        String eligibilityStatus,
        String reason,
        LocalDateTime syncedAt) {
}
