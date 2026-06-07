package org.chovy.canvas.domain.paidmedia;

import java.time.LocalDateTime;
import java.util.Map;

public record PaidMediaAudienceSyncRunView(
        Long id,
        Long tenantId,
        Long destinationId,
        Long audienceId,
        String provider,
        String status,
        Integer requestedCount,
        Integer eligibleCount,
        Integer skippedCount,
        Integer failedCount,
        String externalOperationId,
        String errorMessage,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime startedAt,
        LocalDateTime finishedAt) {

    public PaidMediaAudienceSyncRunView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
