package org.chovy.canvas.bi.api;

import java.util.Map;

public record BiDatasourceFileMaterializationResult(
        BiDatasourceOnboardingView source,
        BiDatasourceSchemaSnapshotView schemaSnapshot,
        Map<String, Object> dataset,
        Map<String, Object> refreshRun) {

    public BiDatasourceFileMaterializationResult {
        dataset = dataset == null ? Map.of() : Map.copyOf(dataset);
        refreshRun = refreshRun == null ? Map.of() : Map.copyOf(refreshRun);
    }
}
