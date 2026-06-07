package org.chovy.canvas.domain.monitoring;

public record MarketingMonitorTrendSnapshotQuery(
        Long sourceId,
        String brandKey,
        String competitorKey,
        int limit) {
}
