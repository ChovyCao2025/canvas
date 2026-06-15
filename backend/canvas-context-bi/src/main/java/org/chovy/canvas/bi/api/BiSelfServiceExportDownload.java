package org.chovy.canvas.bi.api;

public record BiSelfServiceExportDownload(
        String filename,
        String contentType,
        byte[] bytes) {

    public BiSelfServiceExportDownload {
        bytes = bytes == null ? new byte[0] : bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
