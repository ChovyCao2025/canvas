package org.chovy.canvas.domain.bi.permission;

import java.time.LocalDateTime;

public record BiPermissionRequestView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String requestedAction,
        String requestedBy,
        LocalDateTime requestedAt,
        String reason,
        String status,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewComment,
        Long grantedPermissionId) {
}
