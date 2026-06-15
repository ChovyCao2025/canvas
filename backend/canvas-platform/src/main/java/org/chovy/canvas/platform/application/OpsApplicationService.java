package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.OpsFacade;
import org.chovy.canvas.platform.domain.OpsCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpsApplicationService implements OpsFacade {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final OpsCatalog catalog;

    public OpsApplicationService() {
        this(new OpsCatalog());
    }

    public OpsApplicationService(OpsCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> invalidateCache(Long tenantId, Long canvasId, String actor) {
        return catalog.invalidateCache(safeTenantId(tenantId), canvasId, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rebuildRuntimeState(Long tenantId, String actor) {
        return catalog.rebuildRuntimeState(safeTenantId(tenantId), actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> runtimeStatus(Long tenantId, String role, String actor) {
        return catalog.runtimeStatus(safeTenantId(tenantId), roleOrDefault(role), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> auditEvents(Long tenantId, Integer limit) {
        return catalog.auditEvents(safeTenantId(tenantId), normalizeLimit(limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> emergencyAction(Long tenantId, Long canvasId, String action, Map<String, Object> payload,
                                               String role, String actor) {
        return catalog.emergencyAction(safeTenantId(tenantId), canvasId, action, safePayload(payload),
                roleOrDefault(role), actorOrDefault(actor));
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static String roleOrDefault(String role) {
        return role == null || role.isBlank() ? "OPERATOR" : role.trim().toUpperCase();
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static int normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 50 : Math.min(limit, 500);
    }
}
