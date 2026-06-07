package org.chovy.canvas.domain.bi.datasource;

import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractRefreshRunView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResource;

public record BiDatasourceFileMaterializationResult(
        BiDatasourceOnboardingView source,
        BiDatasourceSchemaSnapshotView schemaSnapshot,
        BiDatasetResource dataset,
        BiDatasetAccelerationPolicyView accelerationPolicy,
        BiDatasetExtractRefreshRunView refreshRun
) {
}
