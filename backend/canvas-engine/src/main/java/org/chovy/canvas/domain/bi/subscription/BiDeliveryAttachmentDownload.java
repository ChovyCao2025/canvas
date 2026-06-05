package org.chovy.canvas.domain.bi.subscription;

public record BiDeliveryAttachmentDownload(
        String filename,
        String contentType,
        byte[] bytes
) {
}
