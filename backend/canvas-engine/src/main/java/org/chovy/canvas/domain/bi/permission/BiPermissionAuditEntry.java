package org.chovy.canvas.domain.bi.permission;

import java.time.LocalDateTime;

public record BiPermissionAuditEntry(
        Long id,
        String actorId,
        String actionKey,
        String resourceType,
        String detailJson,
        LocalDateTime createdAt
) {
}
