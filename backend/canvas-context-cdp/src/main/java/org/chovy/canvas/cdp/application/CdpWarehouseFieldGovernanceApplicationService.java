package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseFieldGovernanceFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseFieldGovernanceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpWarehouseFieldGovernanceApplicationService implements CdpWarehouseFieldGovernanceFacade {

    private final CdpWarehouseFieldGovernanceCatalog catalog = new CdpWarehouseFieldGovernanceCatalog();

    @Override
    public List<Map<String, Object>> listPolicies(Long tenantId, String datasetKey, String lifecycleStatus) {
        return catalog.listPolicies(tenantIdOrDefault(tenantId), datasetKey, lifecycleStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertPolicy(Long tenantId, Map<String, Object> payload) {
        return catalog.upsertPolicy(tenantIdOrDefault(tenantId), safePayload(payload));
    }

    @Override
    public Map<String, Object> evaluateBiQuery(Long tenantId, String actor, String role, Map<String, Object> request) {
        return catalog.evaluateBiQuery(tenantIdOrDefault(tenantId), actorOrDefault(actor), roleOrDefault(role),
                safePayload(request));
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static String roleOrDefault(String role) {
        return role == null || role.isBlank() ? "OPERATOR" : role.trim();
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }
}
