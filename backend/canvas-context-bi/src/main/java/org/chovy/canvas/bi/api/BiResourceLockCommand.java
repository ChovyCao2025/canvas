package org.chovy.canvas.bi.api;

public record BiResourceLockCommand(
        String resourceType,
        String resourceKey,
        String lockToken,
        Integer ttlSeconds) {
}
