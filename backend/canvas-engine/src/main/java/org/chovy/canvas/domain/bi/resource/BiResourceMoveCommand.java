package org.chovy.canvas.domain.bi.resource;

public record BiResourceMoveCommand(
        String resourceType,
        String resourceKey,
        String folderKey,
        Integer sortOrder) {
}
