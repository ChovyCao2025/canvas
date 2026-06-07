package org.chovy.canvas.domain.bi.dataset;

public record BiQuickEngineCapacityCategoryUsageView(
        String type,
        long usedRows,
        int resourceCount) {
}
