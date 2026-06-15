package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;

public record BiPermissionAuditEntryView(
        Long id,
        String actorId,
        String actionKey,
        String resourceType,
        String detailJson,
        LocalDateTime createdAt) {
}
