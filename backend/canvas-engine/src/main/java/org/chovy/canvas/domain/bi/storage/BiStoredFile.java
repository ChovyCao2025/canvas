package org.chovy.canvas.domain.bi.storage;

public record BiStoredFile(
        String provider,
        String key,
        String path,
        Long sizeBytes) {
}
