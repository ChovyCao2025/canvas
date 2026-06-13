package org.chovy.canvas.bi.domain;

import java.util.List;

public interface BiDatasetRepository {

    BiDataset findDatasetByKey(Long tenantId, Long workspaceId, BiResourceKey datasetKey);

    BiDataset findDatasetById(Long tenantId, Long datasetId);

    List<BiDataset> listAvailableDatasets(Long tenantId);

    BiDataset findAvailableDatasetByKeyWithTenantFallback(Long tenantId, BiResourceKey datasetKey);

    BiDataset saveDataset(BiDataset dataset);
}
