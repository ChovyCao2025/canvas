package org.chovy.canvas.domain.bi.resource;

import java.time.LocalDateTime;

public record BiResourceCommentView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String widgetKey,
        String commentText,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime deletedAt) {
}
