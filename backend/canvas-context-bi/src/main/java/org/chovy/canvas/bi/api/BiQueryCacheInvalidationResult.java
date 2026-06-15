package org.chovy.canvas.bi.api;

public record BiQueryCacheInvalidationResult(
        int checked,
        int invalidated,
        String status) {
}
