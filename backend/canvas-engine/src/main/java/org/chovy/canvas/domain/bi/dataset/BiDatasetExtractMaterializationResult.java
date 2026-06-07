package org.chovy.canvas.domain.bi.dataset;

public record BiDatasetExtractMaterializationResult(
        String materializedTable,
        Long rowCount,
        Long durationMs) {
}
