package org.chovy.canvas.domain.bi.query;

public record BiQueryCacheInvalidationResult(
        String scope,
        int deletedEntries,
        String message) {
}
