package org.chovy.canvas.bi.api;

public record BiResourceMoveCommand(
        String resourceType,
        String resourceKey,
        String folderKey,
        Integer sortOrder) {
}
