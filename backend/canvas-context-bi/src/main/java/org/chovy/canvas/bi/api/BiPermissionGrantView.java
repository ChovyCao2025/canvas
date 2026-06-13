package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;

public record BiPermissionGrantView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        Long resourceId,
        String subjectType,
        String subjectId,
        String actionKey,
        String effect,
        String createdBy,
        LocalDateTime createdAt
) {
}
