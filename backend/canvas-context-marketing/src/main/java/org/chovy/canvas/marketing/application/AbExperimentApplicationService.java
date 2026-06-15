package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.AbExperimentFacade;
import org.chovy.canvas.marketing.domain.AbExperimentCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AbExperimentApplicationService implements AbExperimentFacade {

    private final AbExperimentCatalog catalog;

    public AbExperimentApplicationService() {
        this(Clock.systemDefaultZone());
    }

    AbExperimentApplicationService(Clock clock) {
        this.catalog = new AbExperimentCatalog(clock);
    }

    @Override
    public Map<String, Object> list(Long tenantId, Map<String, Object> query) {
        return catalog.list(safeTenantId(tenantId), query);
    }

    @Override
    public Map<String, Object> listExperiments(Long tenantId, Integer page, Integer size, Boolean enabled) {
        return catalog.list(safeTenantId(tenantId), page, size, enabled);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
        return createExperiment(tenantId, payload, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createExperiment(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.create(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return updateExperiment(tenantId, id, payload, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateExperiment(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.update(safeTenantId(tenantId), id, payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> delete(Long tenantId, Long id) {
        return deleteExperiment(tenantId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteExperiment(Long tenantId, Long id) {
        return catalog.delete(safeTenantId(tenantId), id);
    }

    @Override
    public List<Map<String, Object>> listGroups(Long tenantId, Long experimentId, boolean includeDisabled) {
        return catalog.listGroups(safeTenantId(tenantId), experimentId, includeDisabled);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createGroup(Long tenantId, Long experimentId, Map<String, Object> payload,
                                           String actor) {
        return catalog.createGroup(safeTenantId(tenantId), experimentId, payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateGroup(Long tenantId, Long experimentId, Long groupId,
                                           Map<String, Object> payload, String actor) {
        return catalog.updateGroup(safeTenantId(tenantId), experimentId, groupId, payload, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteGroup(Long tenantId, Long experimentId, Long groupId) {
        return catalog.deleteGroup(safeTenantId(tenantId), experimentId, groupId);
    }

    @Override
    public Map<String, Object> evaluateGovernance(Long tenantId, Long experimentId, String controlVariantKey,
                                                  String actor) {
        return catalog.evaluateGovernance(safeTenantId(tenantId), experimentId, controlVariantKey,
                actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> evaluateGovernance(Long tenantId, Long experimentId, String controlVariantKey) {
        return evaluateGovernance(tenantId, experimentId, controlVariantKey, "system");
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
