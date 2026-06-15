package org.chovy.canvas.bi.api;

public record BiResourceTransferCommand(
        String resourceType,
        String resourceKey,
        String ownerUser) {
}
