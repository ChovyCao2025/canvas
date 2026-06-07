package org.chovy.canvas.domain.bi.query;

public record BiQueryCacheInvalidationCommand(
        String scope,
        String sqlHash,
        String datasetKey) {
}
