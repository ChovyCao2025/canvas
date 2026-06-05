package org.chovy.canvas.domain.bi.export;

public record BiExportDownload(
        String filename,
        String contentType,
        byte[] bytes) {
}
