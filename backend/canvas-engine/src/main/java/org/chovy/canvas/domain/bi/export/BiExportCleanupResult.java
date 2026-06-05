package org.chovy.canvas.domain.bi.export;

public record BiExportCleanupResult(
        int checked,
        int expired,
        int filesDeleted,
        int failed
) {
}
