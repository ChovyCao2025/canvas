package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

public interface CdpWarehouseCatalogFacade {

    List<Map<String, Object>> listDatasets(Long tenantId, String layer, String status);

    Map<String, Object> upsertDataset(Long tenantId, DatasetCommand command);

    Map<String, Object> createLineageEdge(Long tenantId, LineageCommand command);

    Map<String, Object> lineage(Long tenantId, String datasetKey, Direction direction);

    Map<String, Object> transitiveLineage(Long tenantId, String datasetKey, Direction direction, Integer maxDepth);

    enum Direction {
        UPSTREAM,
        DOWNSTREAM,
        BOTH
    }

    record DatasetCommand(
            String datasetKey,
            String layer,
            String physicalName,
            String displayName,
            String subjectArea,
            String sourceSystem,
            String ownerName,
            String description,
            Integer freshnessSlaMinutes,
            String piiLevel,
            String status,
            String schemaJson) {
    }

    record LineageCommand(
            String upstreamDatasetKey,
            String downstreamDatasetKey,
            String transformType,
            String transformRef,
            String dependencyType,
            String description,
            Boolean active) {
    }
}
