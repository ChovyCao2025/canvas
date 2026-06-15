package org.chovy.canvas.cdp.application;

import java.time.Clock;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpComputedProfileFacade;
import org.chovy.canvas.cdp.domain.CdpComputedProfileCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpComputedProfileApplicationService implements CdpComputedProfileFacade {

    private final CdpComputedProfileCatalog catalog;

    public CdpComputedProfileApplicationService() {
        this(Clock.systemDefaultZone());
    }

    CdpComputedProfileApplicationService(Clock clock) {
        this.catalog = new CdpComputedProfileCatalog(clock);
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
    public Map<String, Object> preview(Long tenantId, Long id) {
        return catalog.preview(safeTenantId(tenantId), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> activate(Long tenantId, Long id, String actor) {
        return catalog.activate(safeTenantId(tenantId), id, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pause(Long tenantId, Long id, String actor) {
        return catalog.pause(safeTenantId(tenantId), id, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runNow(Long tenantId, Long id, String actor) {
        return catalog.runNow(safeTenantId(tenantId), id, actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> listRuns(Long tenantId, Long id, Integer limit) {
        return catalog.listRuns(safeTenantId(tenantId), id, normalizeLimit(limit));
    }

    @Override
    public Map<String, Object> listChanges(Long tenantId, Long id, String userId, Integer limit) {
        return catalog.listChanges(safeTenantId(tenantId), id, blankToNull(userId), normalizeLimit(limit));
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
