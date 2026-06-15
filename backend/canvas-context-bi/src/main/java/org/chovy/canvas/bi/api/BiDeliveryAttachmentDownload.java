package org.chovy.canvas.bi.api;

public record BiDeliveryAttachmentDownload(
        String filename,
        String contentType,
        byte[] bytes) {

    public BiDeliveryAttachmentDownload {
        bytes = bytes == null ? new byte[0] : bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
