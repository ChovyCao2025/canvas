package org.chovy.canvas.bi.api;

public record BiDeliveryAttachmentCleanupResult(
        int checked,
        int expired,
        int filesDeleted,
        int failed) {
}
