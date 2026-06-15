package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.ChannelConnectorFacade;
import org.chovy.canvas.platform.domain.ChannelConnectorCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelConnectorApplicationService implements ChannelConnectorFacade {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final ChannelConnectorCatalog catalog;

    public ChannelConnectorApplicationService() {
        this(new ChannelConnectorCatalog());
    }

    public ChannelConnectorApplicationService(ChannelConnectorCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<Map<String, Object>> connectors(Long tenantId) {
        return catalog.connectors(safeTenantId(tenantId));
    }

    @Override
    public List<Map<String, Object>> limits(Long tenantId) {
        return catalog.limits(safeTenantId(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateMode(Long tenantId, Long connectorId, Map<String, Object> payload, String actor) {
        Map<String, Object> safePayload = safePayload(payload);
        return catalog.updateMode(safeTenantId(tenantId), connectorId, stringValue(safePayload, "mode"),
                stringValue(safePayload, "reason"), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> healthTest(Long tenantId, Long connectorId) {
        return catalog.healthTest(safeTenantId(tenantId), connectorId);
    }

    @Override
    public Map<String, Object> validateFallback(Long tenantId, Map<String, Object> payload) {
        Map<String, Object> safePayload = safePayload(payload);
        return catalog.validateFallback(safeTenantId(tenantId),
                stringValue(safePayload, "channel"),
                stringValue(safePayload, "provider"),
                stringValue(safePayload, "fallbackChannel"),
                stringValue(safePayload, "fallbackProvider"));
    }

    @Override
    public List<Map<String, Object>> fallbackDecisions(Long tenantId) {
        return catalog.fallbackDecisions(safeTenantId(tenantId));
    }

    @Override
    public List<Map<String, Object>> dedupeRecords(Long tenantId) {
        return catalog.dedupeRecords(safeTenantId(tenantId));
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId <= 0 ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static String stringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
