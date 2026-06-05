package org.chovy.canvas.domain.bi.resource;

import java.time.LocalDateTime;

public record BiPublishApprovalView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String status,
        String reason,
        String requestedBy,
        LocalDateTime requestedAt,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewComment) {
}
