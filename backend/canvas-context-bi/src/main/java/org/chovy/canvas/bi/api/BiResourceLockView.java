package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;

public record BiResourceLockView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String lockToken,
        String lockedBy,
        LocalDateTime lockedAt,
        LocalDateTime expiresAt,
        Boolean locked) {
}
