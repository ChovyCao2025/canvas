package org.chovy.canvas.domain.bi.subscription;

public record BiDeliveryAttachmentCleanupResult(
        int checked,
        int expired,
        int filesDeleted,
        int failed
) {
}
