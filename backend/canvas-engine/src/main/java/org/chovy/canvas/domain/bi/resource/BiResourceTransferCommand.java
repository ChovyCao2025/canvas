package org.chovy.canvas.domain.bi.resource;

public record BiResourceTransferCommand(
        String resourceType,
        String resourceKey,
        String ownerUser) {
}
