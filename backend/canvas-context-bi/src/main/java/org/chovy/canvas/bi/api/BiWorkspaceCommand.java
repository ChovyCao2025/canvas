package org.chovy.canvas.bi.api;

public record BiWorkspaceCommand(
        String workspaceKey,
        String name,
        String description,
        String status
) {
}
