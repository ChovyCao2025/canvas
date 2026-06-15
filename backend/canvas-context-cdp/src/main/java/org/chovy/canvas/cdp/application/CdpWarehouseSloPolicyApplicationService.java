package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseSloPolicyFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseSloPolicyCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpWarehouseSloPolicyApplicationService implements CdpWarehouseSloPolicyFacade {

    private final CdpWarehouseSloPolicyCatalog catalog = new CdpWarehouseSloPolicyCatalog();

    @Override
    public List<Map<String, Object>> listPolicies(Long tenantId, String status) {
        return catalog.listPolicies(tenantIdOrDefault(tenantId), status);
    }

    @Override
    public Map<String, Object> effectivePolicy(Long tenantId, String policyKey) {
        return catalog.effectivePolicy(tenantIdOrDefault(tenantId), policyKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertPolicy(Long tenantId, Map<String, Object> payload) {
        return catalog.upsertPolicy(tenantIdOrDefault(tenantId), payload == null ? Map.of() : payload);
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
}
