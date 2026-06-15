package org.chovy.canvas.marketing.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingFormFacade;
import org.chovy.canvas.marketing.domain.MarketingFormCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingFormApplicationService implements MarketingFormFacade {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final MarketingFormCatalog catalog;

    public MarketingFormApplicationService() {
        this(new MarketingFormCatalog());
    }

    public MarketingFormApplicationService(MarketingFormCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<Map<String, Object>> listForms(Long tenantId) {
        return catalog.listForms(safeTenantId(tenantId));
    }

    @Override
    public Map<String, Object> getForm(Long tenantId, Long id) {
        return catalog.getForm(safeTenantId(tenantId), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createForm(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createForm(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateForm(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateForm(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> setStatus(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.setStatus(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> submissions(Long tenantId, Long formId, Integer limit) {
        return catalog.submissions(safeTenantId(tenantId), formId, normalizedLimit(limit));
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? DEFAULT_TENANT_ID : tenantId;
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 100);
    }
}
