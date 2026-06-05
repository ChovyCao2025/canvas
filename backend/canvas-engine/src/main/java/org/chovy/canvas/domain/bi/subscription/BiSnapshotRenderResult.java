package org.chovy.canvas.domain.bi.subscription;

public record BiSnapshotRenderResult(
        String format,
        String contentType,
        byte[] bytes
) {
    public BiSnapshotRenderResult {
        bytes = bytes == null ? new byte[0] : bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
