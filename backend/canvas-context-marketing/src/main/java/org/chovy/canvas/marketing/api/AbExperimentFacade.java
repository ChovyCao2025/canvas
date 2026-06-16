package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

/**
 * 定义AbExperimentFacade的营销上下文访问契约。
 */
public interface AbExperimentFacade {

    /**
     * 查询列表。
     */
    Map<String, Object> list(Long tenantId, Map<String, Object> query);

    /**
     * 查询experiments列表。
     */
    default Map<String, Object> listExperiments(Long tenantId, Integer page, Integer size, Boolean enabled) {
        Map<String, Object> query = new java.util.LinkedHashMap<>();
        if (page != null) {
            query.put("page", page);
        }
        if (size != null) {
            query.put("size", size);
        }
        if (enabled != null) {
            query.put("enabled", enabled ? 1 : 0);
        }
        return list(tenantId, query);
    }

    /**
     * 创建业务对象。
     */
    Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 创建experiment业务对象。
     */
    default Map<String, Object> createExperiment(Long tenantId, Map<String, Object> payload, String actor) {
        return create(tenantId, payload, actor);
    }

    /**
     * 更新业务对象。
     */
    Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor);

    /**
     * 更新experiment业务对象。
     */
    default Map<String, Object> updateExperiment(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return update(tenantId, id, payload, actor);
    }

    /**
     * 删除或停用业务对象。
     */
    Map<String, Object> delete(Long tenantId, Long id);

    /**
     * 删除或停用experiment业务对象。
     */
    default Map<String, Object> deleteExperiment(Long tenantId, Long id) {
        return delete(tenantId, id);
    }

    /**
     * 查询groups列表。
     */
    List<Map<String, Object>> listGroups(Long tenantId, Long experimentId, boolean includeDisabled);

    /**
     * 创建group业务对象。
     */
    Map<String, Object> createGroup(Long tenantId, Long experimentId, Map<String, Object> payload, String actor);

    /**
     * 更新group业务对象。
     */
    Map<String, Object> updateGroup(Long tenantId, Long experimentId, Long groupId, Map<String, Object> payload,
                                    String actor);

    /**
     * 删除或停用group业务对象。
     */
    Map<String, Object> deleteGroup(Long tenantId, Long experimentId, Long groupId);

    /**
     * 执行evaluateGovernance业务操作。
     */
    Map<String, Object> evaluateGovernance(Long tenantId, Long experimentId, String controlVariantKey, String actor);

    /**
     * 执行evaluateGovernance业务操作。
     */
    default Map<String, Object> evaluateGovernance(Long tenantId, Long experimentId, String controlVariantKey) {
        return evaluateGovernance(tenantId, experimentId, controlVariantKey, "system");
    }

}
