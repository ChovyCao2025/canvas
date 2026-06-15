package org.chovy.canvas.bi.api;

public record BiResourcePermissionCommand(
        Long workspaceId,
        String resourceType,
        String resourceKey,
        Long resourceId,
        String subjectType,
        String subjectId,
        String actionKey,
        String effect) {

    public BiResourcePermissionCommand(
            String resourceType,
            String resourceKey,
            Long resourceId,
            String subjectType,
            String subjectId,
            String actionKey,
            String effect) {
        this(null, resourceType, resourceKey, resourceId, subjectType, subjectId, actionKey, effect);
    }
}
