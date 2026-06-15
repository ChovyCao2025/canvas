package org.chovy.canvas.cdp.application;

import java.time.Clock;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpComputedTagFacade;
import org.chovy.canvas.cdp.domain.CdpComputedTagCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpComputedTagApplicationService implements CdpComputedTagFacade {

    private final CdpComputedTagCatalog catalog;

    public CdpComputedTagApplicationService() {
        this(Clock.systemDefaultZone());
    }

    CdpComputedTagApplicationService(Clock clock) {
        this.catalog = new CdpComputedTagCatalog(clock);
    }

    @Override
    public Map<String, Object> list(Long tenantId) {
        return catalog.list(safeTenantId(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.create(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> preview(Long tenantId, String tagCode) {
        return catalog.preview(safeTenantId(tenantId), tagCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> activate(Long tenantId, String tagCode, String actor) {
        return catalog.activate(safeTenantId(tenantId), tagCode, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pause(Long tenantId, String tagCode, String actor) {
        return catalog.pause(safeTenantId(tenantId), tagCode, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runNow(Long tenantId, String tagCode, String actor) {
        return catalog.runNow(safeTenantId(tenantId), tagCode, actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> listRuns(Long tenantId, String tagCode, Integer limit) {
        return catalog.listRuns(safeTenantId(tenantId), tagCode, normalizeLimit(limit));
    }

    @Override
    public Map<String, Object> lineage(Long tenantId, String tagCode) {
        return catalog.lineage(safeTenantId(tenantId), tagCode);
    }

    @Override
    public Map<String, Object> impactCheck(Long tenantId, String tagCode, Map<String, Object> payload, String actor) {
        return catalog.impactCheck(safeTenantId(tenantId), tagCode, safePayload(payload), actorOrDefault(actor));
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static Integer normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
    }
}
