package org.chovy.canvas.domain.bi.permission;

import java.time.LocalDateTime;

public record BiResourcePermissionView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        Long resourceId,
        String subjectType,
        String subjectId,
        String actionKey,
        String effect,
        String createdBy,
        LocalDateTime createdAt) {
}
