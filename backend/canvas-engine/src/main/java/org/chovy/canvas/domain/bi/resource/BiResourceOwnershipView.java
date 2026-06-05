package org.chovy.canvas.domain.bi.resource;

import java.time.LocalDateTime;

public record BiResourceOwnershipView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String ownerUser,
        String transferredBy,
        LocalDateTime transferredAt) {
}
