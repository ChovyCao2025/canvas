package org.chovy.canvas.cdp.application;

import java.time.Clock;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWebhookFacade;
import org.chovy.canvas.cdp.domain.CdpWebhookCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CdpWebhookApplicationService implements CdpWebhookFacade {

    private final CdpWebhookCatalog catalog;

    public CdpWebhookApplicationService() {
        this(Clock.systemDefaultZone());
    }

    CdpWebhookApplicationService(Clock clock) {
        this.catalog = new CdpWebhookCatalog(clock);
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
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.update(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pause(Long tenantId, Long id, String actor) {
        return catalog.transition(safeTenantId(tenantId), id, "PAUSED", actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> resume(Long tenantId, Long id, String actor) {
        return catalog.transition(safeTenantId(tenantId), id, "ACTIVE", actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> disable(Long tenantId, Long id, String actor) {
        return catalog.transition(safeTenantId(tenantId), id, "DISABLED", actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rotateSecret(Long tenantId, Long id, String actor) {
        return catalog.rotateSecret(safeTenantId(tenantId), id, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> testDelivery(Long tenantId, Long id, String actor) {
        return catalog.testDelivery(safeTenantId(tenantId), id, actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> deliveries(Long tenantId, Long id) {
        return deliveries(tenantId, id, null);
    }

    @Override
    public Map<String, Object> deliveries(Long tenantId, Long id, Integer limit) {
        return catalog.deliveries(safeTenantId(tenantId), id, normalizeLimit(limit));
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
