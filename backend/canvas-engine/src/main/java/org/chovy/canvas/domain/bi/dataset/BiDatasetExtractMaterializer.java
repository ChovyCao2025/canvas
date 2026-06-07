package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;

public interface BiDatasetExtractMaterializer {

    BiDatasetExtractMaterializationResult materialize(Long tenantId,
                                                      BiDatasetSpec datasetSpec,
                                                      BiDatasetAccelerationPolicyView policy);

    default boolean dropMaterializedTable(String materializedTable) {
        return false;
    }

    static BiDatasetExtractMaterializer unavailable() {
        return new BiDatasetExtractMaterializer() {
            @Override
            public BiDatasetExtractMaterializationResult materialize(Long tenantId,
                                                                     BiDatasetSpec datasetSpec,
                                                                     BiDatasetAccelerationPolicyView policy) {
                throw new IllegalStateException("BI dataset extract materializer is unavailable");
            }
        };
    }
}
