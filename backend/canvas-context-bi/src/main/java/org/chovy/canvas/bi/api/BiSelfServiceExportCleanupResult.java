package org.chovy.canvas.bi.api;

public record BiSelfServiceExportCleanupResult(
        int checked,
        int removed,
        int retained) {
}
