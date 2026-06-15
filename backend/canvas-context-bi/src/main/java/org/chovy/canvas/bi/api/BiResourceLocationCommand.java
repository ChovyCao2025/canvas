package org.chovy.canvas.bi.api;

public record BiResourceLocationCommand(
        String resourceType,
        String resourceKey,
        String folderKey,
        Integer sortOrder) {
}
