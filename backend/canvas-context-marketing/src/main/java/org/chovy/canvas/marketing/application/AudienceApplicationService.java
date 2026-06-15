package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.AudienceFacade;
import org.chovy.canvas.marketing.domain.AudienceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AudienceApplicationService implements AudienceFacade {

    private final AudienceCatalog catalog;

    public AudienceApplicationService() {
        this(Clock.systemDefaultZone());
    }

    AudienceApplicationService(Clock clock) {
        this.catalog = new AudienceCatalog(clock);
    }

    @Override
    public Map<String, Object> list(Long tenantId, Integer page, Integer size) {
        return catalog.list(safeTenantId(tenantId), normalizedPage(page), normalizedSize(size));
    }

    @Override
    public List<Map<String, Object>> sourceFields(String dataSourceType) {
        return catalog.sourceFields(dataSourceType);
    }

    @Override
    public Map<String, Object> preview(Long tenantId, Map<String, Object> payload) {
        return catalog.preview(safeTenantId(tenantId), payload);
    }

    @Override
    public Map<String, Object> get(Long tenantId, Long id) {
        return catalog.get(safeTenantId(tenantId), id);
    }

    @Override
    public List<Map<String, Object>> ready(Long tenantId) {
        return catalog.ready(safeTenantId(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.create(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.update(safeTenantId(tenantId), id, payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> delete(Long tenantId, Long id) {
        return catalog.delete(safeTenantId(tenantId), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> compute(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.compute(safeTenantId(tenantId), id, payload, actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> stat(Long tenantId, Long id) {
        return catalog.stat(safeTenantId(tenantId), id);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static int normalizedPage(Integer page) {
        return page == null ? 1 : Math.max(1, page);
    }

    private static int normalizedSize(Integer size) {
        if (size == null) {
            return 20;
        }
        return Math.max(1, Math.min(size, 100));
    }
}
