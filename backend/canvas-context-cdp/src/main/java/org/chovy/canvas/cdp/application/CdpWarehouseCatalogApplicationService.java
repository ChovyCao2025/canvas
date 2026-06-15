package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseCatalogCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpWarehouseCatalogApplicationService implements CdpWarehouseCatalogFacade {

    private final CdpWarehouseCatalogCatalog catalog;

    public CdpWarehouseCatalogApplicationService() {
        this(new CdpWarehouseCatalogCatalog());
    }

    public CdpWarehouseCatalogApplicationService(CdpWarehouseCatalogCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<Map<String, Object>> listDatasets(Long tenantId, String layer, String status) {
        return catalog.listDatasets(safeTenantId(tenantId), layer, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertDataset(Long tenantId, DatasetCommand command) {
        return catalog.upsertDataset(safeTenantId(tenantId), command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createLineageEdge(Long tenantId, LineageCommand command) {
        return catalog.createLineageEdge(safeTenantId(tenantId), command);
    }

    @Override
    public Map<String, Object> lineage(Long tenantId, String datasetKey, Direction direction) {
        return catalog.lineage(safeTenantId(tenantId), datasetKey, safeDirection(direction));
    }

    @Override
    public Map<String, Object> transitiveLineage(Long tenantId, String datasetKey, Direction direction,
                                                 Integer maxDepth) {
        return catalog.transitiveLineage(safeTenantId(tenantId), datasetKey, safeDirection(direction), maxDepth);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static Direction safeDirection(Direction direction) {
        return direction == null ? Direction.BOTH : direction;
    }
}
