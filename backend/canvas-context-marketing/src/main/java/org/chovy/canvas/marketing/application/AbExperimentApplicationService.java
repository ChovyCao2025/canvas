package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.AbExperimentFacade;
import org.chovy.canvas.marketing.domain.AbExperimentCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排AbExperiment相关的应用层用例。
 */
@Service
public class AbExperimentApplicationService implements AbExperimentFacade {

    /**
     * 承载该应用服务的内存目录。
     */
    private final AbExperimentCatalog catalog;

    /**
     * 创建AbExperimentApplicationService实例。
     */
    public AbExperimentApplicationService() {
        this(Clock.systemDefaultZone());
    }

    AbExperimentApplicationService(Clock clock) {
        this.catalog = new AbExperimentCatalog(clock);
    }

    /**
     * 查询列表。
     */
    @Override
    public Map<String, Object> list(Long tenantId, Map<String, Object> query) {
        return catalog.list(safeTenantId(tenantId), query);
    }

    /**
     * 查询experiments列表。
     */
    @Override
    public Map<String, Object> listExperiments(Long tenantId, Integer page, Integer size, Boolean enabled) {
        return catalog.list(safeTenantId(tenantId), page, size, enabled);
    }

    /**
     * 创建业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
        return createExperiment(tenantId, payload, actor);
    }

    /**
     * 创建experiment业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createExperiment(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.create(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 更新业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return updateExperiment(tenantId, id, payload, actor);
    }

    /**
     * 更新experiment业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateExperiment(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.update(safeTenantId(tenantId), id, payload, actorOrDefault(actor));
    }

    /**
     * 删除或停用业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> delete(Long tenantId, Long id) {
        return deleteExperiment(tenantId, id);
    }

    /**
     * 删除或停用experiment业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteExperiment(Long tenantId, Long id) {
        return catalog.delete(safeTenantId(tenantId), id);
    }

    /**
     * 查询groups列表。
     */
    @Override
    public List<Map<String, Object>> listGroups(Long tenantId, Long experimentId, boolean includeDisabled) {
        return catalog.listGroups(safeTenantId(tenantId), experimentId, includeDisabled);
    }

    /**
     * 创建group业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createGroup(Long tenantId, Long experimentId, Map<String, Object> payload,
                                           String actor) {
        return catalog.createGroup(safeTenantId(tenantId), experimentId, payload, actorOrDefault(actor));
    }

    /**
     * 更新group业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateGroup(Long tenantId, Long experimentId, Long groupId,
                                           Map<String, Object> payload, String actor) {
        return catalog.updateGroup(safeTenantId(tenantId), experimentId, groupId, payload, actorOrDefault(actor));
    }

    /**
     * 删除或停用group业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteGroup(Long tenantId, Long experimentId, Long groupId) {
        return catalog.deleteGroup(safeTenantId(tenantId), experimentId, groupId);
    }

    /**
     * 执行evaluateGovernance业务操作。
     */
    @Override
    public Map<String, Object> evaluateGovernance(Long tenantId, Long experimentId, String controlVariantKey,
                                                  String actor) {
        return catalog.evaluateGovernance(safeTenantId(tenantId), experimentId, controlVariantKey,
                actorOrDefault(actor));
    }

    /**
     * 执行evaluateGovernance业务操作。
     */
    @Override
    public Map<String, Object> evaluateGovernance(Long tenantId, Long experimentId, String controlVariantKey) {
        return evaluateGovernance(tenantId, experimentId, controlVariantKey, "system");
    }

    /**
     * 执行safeTenantId业务操作。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 执行actorOrDefault业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
