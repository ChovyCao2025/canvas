package org.chovy.canvas.bi.api;

public record BiPublishApprovalCommand(
        String resourceType,
        String resourceKey,
        String reason) {
}
