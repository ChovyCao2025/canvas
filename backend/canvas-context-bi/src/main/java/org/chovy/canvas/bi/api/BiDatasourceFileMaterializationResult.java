package org.chovy.canvas.bi.api;

import java.util.Map;
/**
 * BiDatasourceFileMaterializationResult 结果。
 */
public record BiDatasourceFileMaterializationResult(
        /**
         * source 字段值。
         */
        BiDatasourceOnboardingView source,
        /**
         * schemaSnapshot 字段值。
         */
        BiDatasourceSchemaSnapshotView schemaSnapshot,
        /**
         * dataset 字段值。
         */
        Map<String, Object> dataset,
        Map<String, Object> refreshRun) {

    public BiDatasourceFileMaterializationResult {
        dataset = dataset == null ? Map.of() : Map.copyOf(dataset);
        refreshRun = refreshRun == null ? Map.of() : Map.copyOf(refreshRun);
    }
}
