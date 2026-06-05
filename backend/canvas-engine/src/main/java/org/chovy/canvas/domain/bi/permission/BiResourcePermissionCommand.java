package org.chovy.canvas.domain.bi.permission;

public record BiResourcePermissionCommand(
        String resourceType,
        String resourceKey,
        Long resourceId,
        String subjectType,
        String subjectId,
        String actionKey,
        String effect) {
}
