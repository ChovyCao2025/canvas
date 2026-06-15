package org.chovy.canvas.bi.api;

public record BiPermissionRequestCommand(
        String resourceType,
        String resourceKey,
        String requestedAction,
        String reason) {
}
