package org.chovy.canvas.domain.bi.dataset;

public record BiQuickEngineCapacityUserUsageView(
        String user,
        long usedRows,
        int activeTables,
        int resourceCount) {
}
