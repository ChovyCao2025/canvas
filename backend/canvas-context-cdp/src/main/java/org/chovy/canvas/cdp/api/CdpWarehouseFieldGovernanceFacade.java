package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehouseFieldGovernanceFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseFieldGovernanceFacade {

    /**
     * lifecycle Status)。
     */
    List<Map<String, Object>> listPolicies(Long tenantId, String datasetKey, String lifecycleStatus);

    /**
     * payload)。
     */
    Map<String, Object> upsertPolicy(Long tenantId, Map<String, Object> payload);

    /**
     * request)。
     */
    Map<String, Object> evaluateBiQuery(Long tenantId, String actor, String role, Map<String, Object> request);
}
