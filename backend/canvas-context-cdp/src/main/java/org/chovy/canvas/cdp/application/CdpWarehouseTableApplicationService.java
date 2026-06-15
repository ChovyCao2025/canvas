package org.chovy.canvas.cdp.application;

import java.time.Clock;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseTableFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseTableCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpWarehouseTableApplicationService implements CdpWarehouseTableFacade {

    private final CdpWarehouseTableCatalog catalog;

    public CdpWarehouseTableApplicationService() {
        this(Clock.systemUTC());
    }

    public CdpWarehouseTableApplicationService(Clock clock) {
        this.catalog = new CdpWarehouseTableCatalog(clock);
    }

    @Override
    public Map<String, Object> listContracts(Long tenantId, String layer, String lifecycleStatus) {
        return catalog.listContracts(safeTenantId(tenantId), layer, lifecycleStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertContract(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> inspectContract(Long tenantId, String tableKey, String actor, boolean live) {
        return catalog.inspectContract(safeTenantId(tenantId), tableKey, actorOrDefault(actor), live);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> inspectAll(Long tenantId, String actor, boolean live) {
        return catalog.inspectAll(safeTenantId(tenantId), actorOrDefault(actor), live);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> planRemediation(Long tenantId, String tableKey, boolean live, String actor) {
        return catalog.planRemediation(safeTenantId(tenantId), tableKey, live, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> planAllRemediation(Long tenantId, boolean live, String actor) {
        return catalog.planAllRemediation(safeTenantId(tenantId), live, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanIncidents(Long tenantId, boolean live, String actor) {
        return catalog.scanIncidents(safeTenantId(tenantId), live, actorOrDefault(actor));
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
