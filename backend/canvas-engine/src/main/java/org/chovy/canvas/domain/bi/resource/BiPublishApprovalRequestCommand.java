package org.chovy.canvas.domain.bi.resource;

public record BiPublishApprovalRequestCommand(
        String resourceType,
        String resourceKey,
        String reason) {
}
