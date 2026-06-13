package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;

public record BiWorkspaceView(
        Long id,
        Long tenantId,
        String workspaceKey,
        String name,
        String description,
        String status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
