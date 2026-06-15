package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

public interface AbExperimentFacade {

    Map<String, Object> list(Long tenantId, Map<String, Object> query);

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

    Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor);

    default Map<String, Object> createExperiment(Long tenantId, Map<String, Object> payload, String actor) {
        return create(tenantId, payload, actor);
    }

    Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor);

    default Map<String, Object> updateExperiment(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return update(tenantId, id, payload, actor);
    }

    Map<String, Object> delete(Long tenantId, Long id);

    default Map<String, Object> deleteExperiment(Long tenantId, Long id) {
        return delete(tenantId, id);
    }

    List<Map<String, Object>> listGroups(Long tenantId, Long experimentId, boolean includeDisabled);

    Map<String, Object> createGroup(Long tenantId, Long experimentId, Map<String, Object> payload, String actor);

    Map<String, Object> updateGroup(Long tenantId, Long experimentId, Long groupId, Map<String, Object> payload,
                                    String actor);

    Map<String, Object> deleteGroup(Long tenantId, Long experimentId, Long groupId);

    Map<String, Object> evaluateGovernance(Long tenantId, Long experimentId, String controlVariantKey, String actor);

    default Map<String, Object> evaluateGovernance(Long tenantId, Long experimentId, String controlVariantKey) {
        return evaluateGovernance(tenantId, experimentId, controlVariantKey, "system");
    }

}
