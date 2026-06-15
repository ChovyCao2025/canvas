package org.chovy.canvas.bi.api;

public record BiQueryCacheInvalidationCommand(
        String datasetKey,
        String dashboardKey,
        String sqlHash) {
}
