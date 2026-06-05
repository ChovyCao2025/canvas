package org.chovy.canvas.domain.bi.query;

import java.util.List;

public interface BiDatasetSpecResolver {

    BiDatasetSpec dataset(String datasetKey, Long tenantId);

    List<BiDatasetSpec> datasets(Long tenantId);

    static BiDatasetSpecResolver builtIn() {
        return new BiDatasetSpecResolver() {
            @Override
            public BiDatasetSpec dataset(String datasetKey, Long tenantId) {
                return MarketingBiDatasetRegistry.dataset(datasetKey);
            }

            @Override
            public List<BiDatasetSpec> datasets(Long tenantId) {
                return MarketingBiDatasetRegistry.datasets();
            }
        };
    }
}
