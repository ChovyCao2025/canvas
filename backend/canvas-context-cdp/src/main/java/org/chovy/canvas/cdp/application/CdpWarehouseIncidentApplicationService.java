package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseIncidentFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseIncidentCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpWarehouseIncidentApplicationService implements CdpWarehouseIncidentFacade {

    private final CdpWarehouseIncidentCatalog catalog = new CdpWarehouseIncidentCatalog();

    @Override
    public List<Map<String, Object>> listIncidents(Long tenantId, String status, int limit) {
        return catalog.listIncidents(tenantIdOrDefault(tenantId), status, limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean acknowledge(Long tenantId, Long incidentId, String operator) {
        return catalog.acknowledge(tenantIdOrDefault(tenantId), incidentId, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resolve(Long tenantId, Long incidentId, String operator) {
        return catalog.resolve(tenantIdOrDefault(tenantId), incidentId, operator);
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
}
