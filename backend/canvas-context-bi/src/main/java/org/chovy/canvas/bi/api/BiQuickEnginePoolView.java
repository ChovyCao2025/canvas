package org.chovy.canvas.bi.api;

public record BiQuickEnginePoolView(
        String poolKey,
        Integer maxConcurrentQueries,
        Integer queueLimit,
        Integer queueTimeoutSeconds,
        Integer poolWeight) {
}
