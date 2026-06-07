package org.chovy.canvas.domain.bi.dataset;

public record BiQuickEngineConcurrencyQueueView(
        int runningQueries,
        int queuedQueries,
        int blockedQueries,
        int successfulQueries,
        int failedQueries,
        double concurrencyUsagePercent,
        double queueUsagePercent,
        String state) {
}
