package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseIncidentFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseIncidentCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 CdpWarehouseIncident 的应用服务流程。
 */
@Service
public class CdpWarehouseIncidentApplicationService implements CdpWarehouseIncidentFacade {

    /**
     * 执行 CdpWarehouseIncidentCatalog 对应的 CDP 业务操作。
     */
    private final CdpWarehouseIncidentCatalog catalog = new CdpWarehouseIncidentCatalog();

    /**
     * 查询Incidents列表。
     */
    @Override
    public List<Map<String, Object>> listIncidents(Long tenantId, String status, int limit) {
        return catalog.listIncidents(tenantIdOrDefault(tenantId), status, limit);
    }

    /**
     * 执行 acknowledge 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean acknowledge(Long tenantId, Long incidentId, String operator) {
        return catalog.acknowledge(tenantIdOrDefault(tenantId), incidentId, operator);
    }

    /**
     * 执行 resolve 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resolve(Long tenantId, Long incidentId, String operator) {
        return catalog.resolve(tenantIdOrDefault(tenantId), incidentId, operator);
    }

    /**
     * 执行 tenantIdOrDefault 对应的 CDP 业务操作。
     */
    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
}
