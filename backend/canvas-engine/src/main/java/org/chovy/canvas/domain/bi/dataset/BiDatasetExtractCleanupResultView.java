package org.chovy.canvas.domain.bi.dataset;

public record BiDatasetExtractCleanupResultView(
        String datasetKey,
        Integer checkedTables,
        Integer retainedTables,
        Integer droppedTables,
        Integer failedDrops) {
}
