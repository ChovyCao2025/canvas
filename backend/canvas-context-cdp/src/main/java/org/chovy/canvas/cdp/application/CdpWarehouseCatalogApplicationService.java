package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseCatalogCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 CdpWarehouseCatalog 的应用服务流程。
 */
@Service
public class CdpWarehouseCatalogApplicationService implements CdpWarehouseCatalogFacade {

    /**
     * 领域目录组件。
     */
    private final CdpWarehouseCatalogCatalog catalog;

    /**
     * 创建当前组件实例。
     */
    public CdpWarehouseCatalogApplicationService() {
        this(new CdpWarehouseCatalogCatalog());
    }

    /**
     * 创建当前组件实例。
     */
    public CdpWarehouseCatalogApplicationService(CdpWarehouseCatalogCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 查询Datasets列表。
     */
    @Override
    public List<Map<String, Object>> listDatasets(Long tenantId, String layer, String status) {
        return catalog.listDatasets(safeTenantId(tenantId), layer, status);
    }

    /**
     * 执行 upsertDataset 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertDataset(Long tenantId, DatasetCommand command) {
        return catalog.upsertDataset(safeTenantId(tenantId), command);
    }

    /**
     * 创建Lineage Edge。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createLineageEdge(Long tenantId, LineageCommand command) {
        return catalog.createLineageEdge(safeTenantId(tenantId), command);
    }

    /**
     * 执行 lineage 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> lineage(Long tenantId, String datasetKey, Direction direction) {
        return catalog.lineage(safeTenantId(tenantId), datasetKey, safeDirection(direction));
    }

    /**
     * 执行 transitiveLineage 对应的 CDP 业务操作。
     */
    @Override
    public Map<String, Object> transitiveLineage(Long tenantId, String datasetKey, Direction direction,
                                                 Integer maxDepth) {
        return catalog.transitiveLineage(safeTenantId(tenantId), datasetKey, safeDirection(direction), maxDepth);
    }

    /**
     * 返回安全的Tenant Id。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 返回安全的Direction。
     */
    private static Direction safeDirection(Direction direction) {
        return direction == null ? Direction.BOTH : direction;
    }
}
