package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;

public record BiResourceLocationView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String folderKey,
        Integer sortOrder,
        String movedBy,
        LocalDateTime movedAt) {
}
