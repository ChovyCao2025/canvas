package org.chovy.canvas.domain.bi.query;

import java.time.LocalDateTime;

public record BiQueryGovernanceAuditEntry(
        Long id,
        String actorId,
        String actionKey,
        String resourceType,
        String detailJson,
        LocalDateTime createdAt
) {
}
