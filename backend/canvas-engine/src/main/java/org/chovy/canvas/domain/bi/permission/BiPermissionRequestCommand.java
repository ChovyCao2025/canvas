package org.chovy.canvas.domain.bi.permission;

public record BiPermissionRequestCommand(
        String resourceType,
        String resourceKey,
        String requestedAction,
        String reason) {
}
