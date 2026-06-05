package org.chovy.canvas.domain.bi.resource;

public record BiResourceLockCommand(
        String resourceType,
        String resourceKey,
        String lockToken,
        Integer ttlSeconds) {
}
