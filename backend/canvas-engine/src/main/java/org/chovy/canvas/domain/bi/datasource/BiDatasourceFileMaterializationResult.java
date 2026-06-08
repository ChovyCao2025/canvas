package org.chovy.canvas.domain.bi.datasource;

import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractRefreshRunView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResource;

/**
 * BiDatasourceFileMaterializationResult 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param source source 字段。
 * @param schemaSnapshot schemaSnapshot 字段。
 * @param dataset dataset 字段。
 * @param accelerationPolicy accelerationPolicy 字段。
 * @param refreshRun refreshRun 字段。
 */
public record BiDatasourceFileMaterializationResult(
        BiDatasourceOnboardingView source,
        BiDatasourceSchemaSnapshotView schemaSnapshot,
        BiDatasetResource dataset,
        BiDatasetAccelerationPolicyView accelerationPolicy,
        BiDatasetExtractRefreshRunView refreshRun
) {
}
