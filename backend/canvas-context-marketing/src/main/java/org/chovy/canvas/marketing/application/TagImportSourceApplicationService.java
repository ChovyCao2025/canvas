package org.chovy.canvas.marketing.application;

import java.util.Map;

import org.chovy.canvas.marketing.api.TagImportSourceFacade;
import org.chovy.canvas.marketing.domain.TagImportSourceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagImportSourceApplicationService implements TagImportSourceFacade {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final TagImportSourceCatalog catalog;

    public TagImportSourceApplicationService() {
        this(new TagImportSourceCatalog());
    }

    public TagImportSourceApplicationService(TagImportSourceCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Map<String, Object> listSources(Long tenantId, Integer enabled) {
        return catalog.listSources(tenantIdOrDefault(tenantId), enabled);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createSource(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createSource(tenantIdOrDefault(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateSource(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateSource(tenantIdOrDefault(tenantId), id, payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSource(Long tenantId, Long id) {
        catalog.deleteSource(tenantIdOrDefault(tenantId), id);
    }

    @Override
    public Map<String, Object> runSource(Long tenantId, Long id) {
        return catalog.runSource(tenantIdOrDefault(tenantId), id);
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }
}
