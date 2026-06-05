package org.chovy.canvas.domain.bi.subscription;

public record BiEmailAttachment(
        String fileName,
        String contentType,
        byte[] bytes
) {
    public BiEmailAttachment {
        bytes = bytes == null ? new byte[0] : bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
