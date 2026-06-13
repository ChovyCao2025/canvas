package org.chovy.canvas.bi.api;

public record BiPermissionGrantCommand(
        Long workspaceId,
        String resourceType,
        Long resourceId,
        String subjectType,
        String subjectId,
        String actionKey,
        String effect
) {
}
